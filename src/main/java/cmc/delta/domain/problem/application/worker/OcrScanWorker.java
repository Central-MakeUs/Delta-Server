package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.problem.application.scan.port.out.ocr.OcrClient;
import cmc.delta.domain.problem.application.scan.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.application.scan.port.out.storage.ObjectStorageReader;
import cmc.delta.domain.problem.application.worker.properties.OcrWorkerProperties;
import cmc.delta.domain.problem.application.worker.support.AbstractClaimingScanWorker;
import cmc.delta.domain.problem.application.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.application.worker.support.failure.OcrFailureDecider;
import cmc.delta.domain.problem.application.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.application.worker.support.persistence.OcrScanPersister;
import cmc.delta.domain.problem.application.worker.support.validation.OcrScanValidator;
import cmc.delta.domain.problem.model.asset.Asset;
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
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class OcrScanWorker extends AbstractClaimingScanWorker {

	private static final String OCR_FILENAME_PREFIX = "scan-";
	private static final String OCR_FILENAME_SUFFIX = ".jpg";

	private final ProblemScanJpaRepository problemScanRepository;
	private final AssetJpaRepository assetRepository;
	private final ObjectStorageReader storageReader;
	private final OcrClient ocrClient;
	private final OcrWorkerProperties properties;

	private LocalDateTime lastBacklogLoggedAt;

	private final ScanLockGuard lockGuard;
	private final OcrFailureDecider failureDecider;
	private final OcrScanPersister ocrScanPersister;
	private final OcrScanValidator ocrScanValidator;

	public OcrScanWorker(
		Clock clock,
		org.springframework.transaction.support.TransactionTemplate workerTxTemplate,
		@Qualifier("ocrExecutor") Executor ocrExecutor,
		ProblemScanJpaRepository problemScanRepository,
		AssetJpaRepository assetRepository,
		ObjectStorageReader storageReader,
		OcrClient ocrClient,
		OcrWorkerProperties properties
	) {
		super(clock, workerTxTemplate, ocrExecutor);
		this.problemScanRepository = problemScanRepository;
		this.assetRepository = assetRepository;
		this.storageReader = storageReader;
		this.ocrClient = ocrClient;
		this.properties = properties;

		this.lockGuard = new ScanLockGuard(problemScanRepository);
		this.failureDecider = new OcrFailureDecider();
		this.ocrScanPersister = new OcrScanPersister(workerTxTemplate, problemScanRepository);
		this.ocrScanValidator = new OcrScanValidator(assetRepository);
	}

	@Override
	protected int claim(
		LocalDateTime now,
		LocalDateTime staleBefore,
		String lockOwner,
		String lockToken,
		LocalDateTime lockedAt,
		int limit
	) {
		return problemScanRepository.claimOcrCandidates(now, staleBefore, lockOwner, lockToken, lockedAt, limit);
	}

	@Override
	protected List<Long> findClaimedIds(String lockOwner, String lockToken, int limit) {
		return problemScanRepository.findClaimedOcrIds(lockOwner, lockToken, limit);
	}

	@Override
	protected void onNoCandidate(LocalDateTime now) {
		log.debug("OCR 워커 tick - 처리 대상 없음");

		if (!shouldLogBacklog(now)) return;

		LocalDateTime staleBefore = now.minusSeconds(lockLeaseSeconds());
		long backlog = problemScanRepository.countOcrBacklog(now, staleBefore);

		log.info("OCR 워커 - 처리 대상 없음 (ocrBacklog={})", backlog);
		lastBacklogLoggedAt = now;
	}

	private boolean shouldLogBacklog(LocalDateTime now) {
		if (lastBacklogLoggedAt == null) return true;

		Duration interval = Duration.ofMinutes(properties.backlogLogMinutes());
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
			Asset originalAsset = ocrScanValidator.requireOriginalAsset(scanId);

			byte[] originalBytes = storageReader.readBytes(originalAsset.getStorageKey());
			String filename = OCR_FILENAME_PREFIX + scanId + OCR_FILENAME_SUFFIX;

			OcrResult ocrResult = ocrClient.recognize(originalBytes, filename);

			if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) {
				return;
			}

			LocalDateTime completedAt = LocalDateTime.now();
			ocrScanPersister.persistOcrSucceeded(scanId, lockOwner, lockToken, ocrResult, completedAt);

			log.info("OCR 처리 완료 scanId={} 상태=OCR_DONE", scanId);

		} catch (Exception exception) {
			FailureDecision decision = failureDecider.decide(exception);

			LocalDateTime now = LocalDateTime.now();
			ocrScanPersister.persistOcrFailed(scanId, lockOwner, lockToken, decision, now);

			if (exception instanceof RestClientResponseException restClientResponseException
				&& restClientResponseException.getRawStatusCode() >= 400
				&& restClientResponseException.getRawStatusCode() < 500
			) {
				log.warn("OCR 호출 4xx scanId={} reason={} status={}",
					scanId,
					decision.reasonCode().code(),
					restClientResponseException.getRawStatusCode()
				);
			} else {
				log.error("OCR 처리 실패 scanId={} reason={}", scanId, decision.reasonCode().code(), exception);
			}
		} finally {
			unlockBestEffort(scanId, lockOwner, lockToken);
		}
	}

	private void unlockBestEffort(Long scanId, String lockOwner, String lockToken) {
		try {
			org.springframework.transaction.support.TransactionTemplate txTemplate = getWorkerTransactionTemplate();
			txTemplate.executeWithoutResult(status ->
				problemScanRepository.unlock(scanId, lockOwner, lockToken)
			);
		} catch (Exception unlockException) {
			log.error("OCR unlock 실패 scanId={}", scanId, unlockException);
		}
	}

	private org.springframework.transaction.support.TransactionTemplate getWorkerTransactionTemplate() {
		// AbstractClaimingScanWorker에서 txTemplate을 노출하지 않는다면,
		// 기존처럼 workerTxTemplate을 필드로 유지하고 여기서 사용해도 된다.
		throw new UnsupportedOperationException("workerTxTemplate을 필드로 보관해서 사용하세요.");
	}
}
