package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.problem.application.port.ObjectStorageReader;
import cmc.delta.domain.problem.application.port.OcrClient;
import cmc.delta.domain.problem.application.port.OcrResult;
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

@Slf4j
@Component
public class OcrScanWorker extends AbstractScanWorker {

	private final ProblemScanJpaRepository scanRepository;
	private final AssetJpaRepository assetRepository;
	private final ObjectStorageReader storageReader;
	private final OcrClient ocrClient;

	// 생성자 1개만 (롬복 생성자 섞지 말기)
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
		return scanRepository.tryLock(scanId, lockOwner, now) == 1;
	}

	@Override
	protected void processLocked(Long scanId, LocalDateTime now) {
		ProblemScan scan = scanRepository.findById(scanId)
			.orElseThrow(() -> new IllegalStateException("problem_scan not found. id=" + scanId));

		Asset original = assetRepository.findOriginalByScanId(scanId)
			.orElseThrow(() -> new IllegalStateException("ORIGINAL asset not found. scanId=" + scanId));

		byte[] bytes = storageReader.readBytes(original.getStorageKey());
		OcrResult result = ocrClient.recognize(bytes, "scan-" + scanId + ".jpg");

		scan.markOcrSucceeded(
			result.plainText(),
			result.rawJson(),
			"MATHPIX",
			now
		);
		log.info("[OCR] done scanId={} status={}", scanId, scan.getStatus());
	}

	@Override
	protected void handleFailure(Long scanId, LocalDateTime now, Exception e) {
		log.warn("[OCR] failed scanId={}", scanId, e);

		ProblemScan scan = scanRepository.findById(scanId)
			.orElseThrow(() -> new IllegalStateException("problem_scan not found. id=" + scanId));

		scan.markOcrFailed("OCR_FAILED");
		scan.scheduleNextRetryForOcr(now);
	}

	@Override
	protected void unlock(Long scanId, String lockOwner) {
		scanRepository.unlock(scanId, lockOwner);
	}
}
