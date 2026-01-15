package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.problem.application.port.ocr.ObjectStorageReader;
import cmc.delta.domain.problem.application.port.ocr.OcrClient;
import cmc.delta.domain.problem.application.port.ocr.OcrResult;
import cmc.delta.domain.problem.application.worker.support.AbstractClaimingScanWorker;
import cmc.delta.domain.problem.application.worker.support.OcrWorkerProperties;
import cmc.delta.domain.problem.model.Asset;
import cmc.delta.domain.problem.model.ProblemScan;
import cmc.delta.domain.problem.persistence.AssetJpaRepository;
import cmc.delta.domain.problem.persistence.ProblemScanJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class OcrScanWorker extends AbstractClaimingScanWorker {

	private final ProblemScanJpaRepository scanRepository;
	private final AssetJpaRepository assetRepository;
	private final ObjectStorageReader storageReader;
	private final OcrClient ocrClient;
	private final TransactionTemplate template;
	private LocalDateTime lastBacklogLoggedAt;
	private final OcrWorkerProperties props;

	public OcrScanWorker(
		Clock clock,
		TransactionTemplate workerTxTemplate,
		@Qualifier("ocrExecutor") Executor ocrExecutor,
		ProblemScanJpaRepository scanRepository,
		AssetJpaRepository assetRepository,
		ObjectStorageReader storageReader,
		OcrClient ocrClient,
		OcrWorkerProperties props
	) {
		super(clock, workerTxTemplate, ocrExecutor);
		this.template = workerTxTemplate;
		this.scanRepository = scanRepository;
		this.assetRepository = assetRepository;
		this.storageReader = storageReader;
		this.ocrClient = ocrClient;
		this.props = props;
	}

	@Override
	protected int claim(LocalDateTime now, LocalDateTime staleBefore, String lockOwner, String lockToken, LocalDateTime lockedAt, int limit) {
		return scanRepository.claimOcrCandidates(now, staleBefore, lockOwner, lockToken, lockedAt, limit);
	}

	@Override
	protected List<Long> findClaimedIds(String lockOwner, String lockToken, int limit) {
		return scanRepository.findClaimedOcrIds(lockOwner, lockToken, limit);
	}

	@Override
	protected void onNoCandidate(LocalDateTime now) {
		log.debug("OCR 워커 tick - 처리 대상 없음");

		if (!shouldLogBacklog(now)) return;

		LocalDateTime staleBefore = now.minusSeconds(lockLeaseSeconds());
		long backlog = scanRepository.countOcrBacklog(now, staleBefore);

		log.info("OCR 워커 - 처리 대상 없음 (ocrBacklog={})", backlog);
		lastBacklogLoggedAt = now;
	}

	private boolean shouldLogBacklog(LocalDateTime now) {
		if (lastBacklogLoggedAt == null) return true;

		Duration interval = Duration.ofMinutes(props.backlogLogMinutes());
		return !now.isBefore(lastBacklogLoggedAt.plus(interval));
	}

	@Override
	protected void onClaimed(LocalDateTime now, int count) {
		log.info("OCR 워커 tick - 이번 배치 처리 대상={}건", count);
	}

	@Override
	protected void processOne(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
		// 락을 잃었으면(lease 만료 후 다른 워커가 가져감) 외부호출(비용)부터 막기
		if (!isStillLockedByMe(scanId, lockOwner, lockToken)) {
			return;
		}

		try {
			Asset original = assetRepository.findOriginalByScanId(scanId)
				.orElseThrow(() -> new IllegalStateException("ASSET_NOT_FOUND"));

			byte[] bytes = storageReader.readBytes(original.getStorageKey());
			OcrResult result = ocrClient.recognize(bytes, "scan-" + scanId + ".jpg");

			saveOcrSuccess(scanId, result);

			log.info("OCR 처리 완료 scanId={} 상태=OCR_DONE", scanId);
		} catch (Exception e) {
			String reason = classifyFailureReason(e);

			boolean retryable = isRetryable(e);
			saveOcrFailure(scanId, reason, retryable);

			// 4xx는 스택 찍지 말고(노이즈/민감정보 위험), 5xx/네트워크만 스택 포함
			if (e instanceof RestClientResponseException rre && rre.getRawStatusCode() >= 400 && rre.getRawStatusCode() < 500) {
				log.warn("OCR 호출 4xx scanId={} reason={} status={}", scanId, reason, rre.getRawStatusCode());
			} else {
				log.error("OCR 처리 실패 scanId={} reason={}", scanId, reason, e);
			}
		} finally {
			unlockBestEffort(scanId, lockOwner, lockToken);
		}
	}

	private boolean isStillLockedByMe(Long scanId, String lockOwner, String lockToken) {
		Integer exists = scanRepository.existsLockedBy(scanId, lockOwner, lockToken);
		return exists != null;
	}

	private void saveOcrSuccess(Long scanId, OcrResult result) {
		template.executeWithoutResult(status -> {
			ProblemScan scan = scanRepository.findById(scanId)
				.orElseThrow(() -> new IllegalStateException("SCAN_NOT_FOUND"));
			scan.markOcrSucceeded(result.plainText(), result.rawJson(), LocalDateTime.now());
		});
	}

	private void saveOcrFailure(Long scanId, String reason, boolean retryable) {
		template.executeWithoutResult(status -> {
			ProblemScan scan = scanRepository.findById(scanId).orElse(null);
			if (scan == null) return;

			scan.markOcrFailed(reason);

			if (!retryable) {
				scan.markFailed(reason);
				return;
			}

			scan.scheduleNextRetryForOcr(LocalDateTime.now());
		});
	}

	private void unlockBestEffort(Long scanId, String lockOwner, String lockToken) {
		try {
			template.executeWithoutResult(status -> scanRepository.unlock(scanId, lockOwner, lockToken));

		} catch (Exception unlockEx) {
			log.error("OCR unlock 실패 scanId={}", scanId, unlockEx);
		}
	}

	private boolean isRetryable(Exception e) {
		if (e instanceof IllegalStateException ise) {
			String msg = ise.getMessage();
			if ("ASSET_NOT_FOUND".equals(msg)) return false;
			if ("SCAN_NOT_FOUND".equals(msg)) return false;
			return false; // 내부 상태/데이터 이상은 재시도 X로 보수적으로
		}

		if (e instanceof ResourceAccessException) return true;
		if (e instanceof RestClientResponseException rre) {
			int s = rre.getRawStatusCode();
			if (s == 429) return true;
			if (s >= 500) return true;
			return false;
		}
		return true;
	}

	private String classifyFailureReason(Exception e) {
		if (e instanceof IllegalStateException ise) {
			String msg = ise.getMessage();
			if ("ASSET_NOT_FOUND".equals(msg)) return "ASSET_NOT_FOUND";
			if ("SCAN_NOT_FOUND".equals(msg)) return "SCAN_NOT_FOUND";
			return "ILLEGAL_STATE";
		}
		if (e instanceof RestClientResponseException rre) {
			int status = rre.getRawStatusCode();
			if (status == 429) return "OCR_RATE_LIMIT";
			if (status >= 400 && status < 500) return "OCR_CLIENT_4XX";
			if (status >= 500) return "OCR_CLIENT_5XX";
			return "OCR_CLIENT_ERROR";
		}
		if (e instanceof ResourceAccessException) return "OCR_NETWORK_ERROR";
		return "OCR_FAILED";
	}
}
