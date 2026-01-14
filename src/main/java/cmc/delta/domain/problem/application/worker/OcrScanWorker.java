package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.problem.application.port.ocr.ObjectStorageReader;
import cmc.delta.domain.problem.application.port.ocr.OcrClient;
import cmc.delta.domain.problem.application.port.ocr.OcrResult;
import cmc.delta.domain.problem.application.worker.support.AbstractScanWorker;
import cmc.delta.domain.problem.model.Asset;
import cmc.delta.domain.problem.model.ProblemScan;
import cmc.delta.domain.problem.persistence.AssetJpaRepository;
import cmc.delta.domain.problem.persistence.ProblemScanJpaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * OCR 스캔 작업자 구현체
 * Refactor예정 [ ]
 */
@Slf4j
@Component
public class OcrScanWorker extends AbstractScanWorker {

	private final ProblemScanJpaRepository scanRepository;
	private final AssetJpaRepository assetRepository;
	private final ObjectStorageReader storageReader;
	private final OcrClient ocrClient;

	public OcrScanWorker(
		Clock clock,
		ProblemScanJpaRepository scanRepository,
		AssetJpaRepository assetRepository,
		ObjectStorageReader storageReader,
		OcrClient ocrClient
	) {
		super(clock);
		this.scanRepository = scanRepository;
		this.assetRepository = assetRepository;
		this.storageReader = storageReader;
		this.ocrClient = ocrClient;
	}

	@Transactional
	public void runOnce(String lockOwner) {
		super.runOnceInternal(lockOwner);
	}

	@Override
	protected Long pickCandidateId(LocalDateTime now) {
		return scanRepository.findNextOcrCandidateId(now).orElse(null);
	}

	@Override
	protected boolean tryLock(Long scanId, String lockOwner, LocalDateTime now) {
		return scanRepository.tryLockOcrCandidate(scanId, lockOwner, now) == 1;
	}

	@Override
	protected void processLocked(Long scanId, LocalDateTime now) {
		ProblemScan scan = scanRepository.findById(scanId)
			.orElseThrow(() -> new IllegalStateException("problem_scan not found. id=" + scanId));

		Asset original = assetRepository.findOriginalByScanId(scanId)
			.orElseThrow(() -> new IllegalStateException("ORIGINAL asset not found. scanId=" + scanId));

		// 민감정보(스토리지키/프리사인URL/바이트) 로그 금지
		byte[] bytes = storageReader.readBytes(original.getStorageKey());

		// 원본 파일명/URL 금지 → 내부용 안전한 이름만
		OcrResult result = ocrClient.recognize(bytes, "scan-" + scanId + ".jpg");

		scan.markOcrSucceeded(result.plainText(), result.rawJson(), now);

		log.info("OCR 처리 완료 scanId={} 상태={}", scanId, scan.getStatus());
	}

	@Override
	protected void handleFailure(Long scanId, LocalDateTime now, Exception e) {
		String reason = classifyFailureReason(e);

		// 4xx는 WARN(스택 생략), 그 외는 ERROR(스택 포함)
		if (e instanceof RestClientResponseException rre && rre.getRawStatusCode() >= 400 && rre.getRawStatusCode() < 500) {
			log.warn("OCR 처리 실패(클라이언트 오류) scanId={} 원인={} httpStatus={}",
				scanId, reason, rre.getRawStatusCode());
		} else if (isExpectedNoStackReason(reason)) {
			log.warn("OCR 처리 실패(예상 가능) scanId={} 원인={}", scanId, reason);
		} else {
			log.error("OCR 처리 실패 scanId={} 원인={}", scanId, reason, e);
		}

		ProblemScan scan = scanRepository.findById(scanId).orElse(null);
		if (scan == null) {
			log.error("OCR 실패 후 상태 업데이트 불가: 스캔이 존재하지 않음 scanId={}", scanId);
			return;
		}

		scan.markOcrFailed(reason);
		scan.scheduleNextRetryForOcr(now);

		log.warn("OCR 재시도 예약 scanId={} 시도횟수={} 다음재시도시각={} 원인={}",
			scanId, scan.getOcrAttemptCount(), scan.getNextRetryAt(), reason);
	}

	private boolean isExpectedNoStackReason(String reason) {
		return "ASSET_NOT_FOUND".equals(reason) || "SCAN_NOT_FOUND".equals(reason);
	}

	private String classifyFailureReason(Exception e) {
		if (e instanceof IllegalStateException ise) {
			String msg = ise.getMessage();
			if (msg != null && msg.contains("ORIGINAL asset not found")) return "ASSET_NOT_FOUND";
			if (msg != null && msg.contains("problem_scan not found")) return "SCAN_NOT_FOUND";
			return "ILLEGAL_STATE";
		}

		if (e instanceof RestClientResponseException rre) {
			int status = rre.getRawStatusCode();
			if (status >= 400 && status < 500) return "OCR_CLIENT_4XX";
			if (status >= 500) return "OCR_CLIENT_5XX";
			return "OCR_CLIENT_ERROR";
		}

		if (e instanceof ResourceAccessException) {
			return "OCR_NETWORK_ERROR"; // timeout/connection
		}

		return "OCR_FAILED";
	}

	@Override
	protected void unlock(Long scanId, String lockOwner) {
		scanRepository.unlock(scanId, lockOwner);
	}
}
