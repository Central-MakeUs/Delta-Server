package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.problem.application.scan.port.out.storage.ObjectStorageReader;
import cmc.delta.domain.problem.application.scan.port.out.ocr.OcrClient;
import cmc.delta.domain.problem.application.scan.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.application.worker.support.AbstractClaimingScanWorker;
import cmc.delta.domain.problem.application.worker.properties.OcrWorkerProperties;
import cmc.delta.domain.problem.application.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.application.worker.support.failure.OcrFailureDecider;
import cmc.delta.domain.problem.application.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class OcrScanWorker extends AbstractClaimingScanWorker {

	private static final String OCR_FILENAME_PREFIX = "scan-";
	private static final String OCR_FILENAME_SUFFIX = ".jpg";

	private final ProblemScanJpaRepository scanRepository;
	private final AssetJpaRepository assetRepository;
	private final ObjectStorageReader storageReader;
	private final OcrClient ocrClient;
	private final TransactionTemplate template;
	private LocalDateTime lastBacklogLoggedAt;
	private final OcrWorkerProperties props;

	private final ScanLockGuard lockGuard;
	private final OcrFailureDecider failureDecider;

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

		this.lockGuard = new ScanLockGuard(scanRepository);
		this.failureDecider = new OcrFailureDecider();
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
		if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) {
			return;
		}

		try {
			Asset original = assetRepository.findOriginalByScanId(scanId)
				.orElseThrow(() -> new IllegalStateException("ASSET_NOT_FOUND"));

			byte[] bytes = storageReader.readBytes(original.getStorageKey());
			String filename = OCR_FILENAME_PREFIX + scanId + OCR_FILENAME_SUFFIX;

			OcrResult result = ocrClient.recognize(bytes, filename);

			saveOcrSuccess(scanId, lockOwner, lockToken, result);

			log.info("OCR 처리 완료 scanId={} 상태=OCR_DONE", scanId);
		} catch (Exception e) {
			FailureDecision decision = failureDecider.decide(e);
			saveOcrFailure(scanId, lockOwner, lockToken, decision);

			if (e instanceof RestClientResponseException rre && rre.getRawStatusCode() >= 400 && rre.getRawStatusCode() < 500) {
				log.warn("OCR 호출 4xx scanId={} reason={} status={}", scanId, decision.reasonCode().name(), rre.getRawStatusCode());
			} else {
				log.error("OCR 처리 실패 scanId={} reason={}", scanId, decision.reasonCode().name(), e);
			}
		} finally {
			unlockBestEffort(scanId, lockOwner, lockToken);
		}
	}

	private void saveOcrSuccess(Long scanId, String lockOwner, String lockToken, OcrResult result) {
		template.executeWithoutResult(status -> {
			if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) return;

			ProblemScan scan = scanRepository.findById(scanId)
				.orElseThrow(() -> new IllegalStateException("SCAN_NOT_FOUND"));

			scan.markOcrSucceeded(result.plainText(), result.rawJson(), LocalDateTime.now());
		});
	}

	private void saveOcrFailure(Long scanId, String lockOwner, String lockToken, FailureDecision decision) {
		template.executeWithoutResult(status -> {
			if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) return;

			ProblemScan scan = scanRepository.findById(scanId).orElse(null);
			if (scan == null) return;

			String reason = decision.reasonCode().name();
			scan.markOcrFailed(reason);

			if (!decision.retryable()) {
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
}
