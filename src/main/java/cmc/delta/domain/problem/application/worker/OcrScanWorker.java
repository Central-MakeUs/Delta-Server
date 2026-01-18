package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.problem.application.scan.port.out.ocr.OcrClient;
import cmc.delta.domain.problem.application.scan.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.application.scan.port.out.storage.ObjectStorageReader;
import cmc.delta.domain.problem.application.worker.properties.OcrWorkerProperties;
import cmc.delta.domain.problem.application.worker.support.AbstractClaimingScanWorker;
import cmc.delta.domain.problem.application.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.application.worker.support.failure.OcrFailureDecider;
import cmc.delta.domain.problem.application.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.application.worker.support.lock.ScanUnlocker;
import cmc.delta.domain.problem.application.worker.support.logging.BacklogLogger;
import cmc.delta.domain.problem.application.worker.support.logging.WorkerLogPolicy;
import cmc.delta.domain.problem.application.worker.support.persistence.OcrScanPersister;
import cmc.delta.domain.problem.application.worker.support.validation.OcrScanValidator;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.persistence.scan.ScanRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class OcrScanWorker extends AbstractClaimingScanWorker {

	private static final String WORKER_KEY = "worker:ocr:backlog";
	private static final String OCR_FILENAME_PREFIX = "scan-";
	private static final String OCR_FILENAME_SUFFIX = ".jpg";

	private final ScanRepository problemScanRepository;
	private final ObjectStorageReader storageReader;
	private final OcrClient ocrClient;
	private final OcrWorkerProperties properties;

	private final ScanLockGuard lockGuard;
	private final ScanUnlocker unlocker;
	private final BacklogLogger backlogLogger;
	private final WorkerLogPolicy logPolicy;

	private final OcrFailureDecider failureDecider;
	private final OcrScanValidator validator;
	private final OcrScanPersister persister;

	public OcrScanWorker(
		Clock clock,
		TransactionTemplate workerTxTemplate,
		@Qualifier("ocrExecutor") Executor ocrExecutor,
		ScanRepository problemScanRepository,
		ObjectStorageReader storageReader,
		OcrClient ocrClient,
		OcrWorkerProperties properties,
		ScanLockGuard lockGuard,
		ScanUnlocker unlocker,
		BacklogLogger backlogLogger,
		WorkerLogPolicy logPolicy,
		OcrFailureDecider failureDecider,
		OcrScanValidator validator,
		OcrScanPersister persister
	) {
		super(clock, workerTxTemplate, ocrExecutor, "ocr");
		this.problemScanRepository = problemScanRepository;
		this.storageReader = storageReader;
		this.ocrClient = ocrClient;
		this.properties = properties;

		this.lockGuard = lockGuard;
		this.unlocker = unlocker;
		this.backlogLogger = backlogLogger;
		this.logPolicy = logPolicy;

		this.failureDecider = failureDecider;
		this.validator = validator;
		this.persister = persister;
	}

	@Override
	protected int claim(LocalDateTime now, LocalDateTime staleBefore, String lockOwner, String lockToken, LocalDateTime lockedAt, int limit) {
		return problemScanRepository.claimOcrCandidates(now, staleBefore, lockOwner, lockToken, lockedAt, limit);
	}

	@Override
	protected List<Long> findClaimedIds(String lockOwner, String lockToken, int limit) {
		return problemScanRepository.findClaimedOcrIds(lockOwner, lockToken, limit);
	}

	@Override
	protected void onNoCandidate(LocalDateTime now) {
		log.debug("OCR 워커 tick - 처리 대상 없음");

		LocalDateTime staleBefore = now.minusSeconds(lockLeaseSeconds());
		backlogLogger.logIfDue(
			WORKER_KEY,
			now,
			properties.backlogLogMinutes(),
			() -> problemScanRepository.countOcrBacklog(now, staleBefore),
			(backlog) -> log.info("OCR 워커 - 처리 대상 없음 (ocrBacklog={})", backlog)
		);
	}

	@Override
	protected void onClaimed(LocalDateTime now, int count) {
		log.info("OCR 워커 tick - 이번 배치 처리 대상={}건", count);
	}

	@Override
	protected void processOne(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
		if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) return;

		try {
			Asset originalAsset = validator.requireOriginalAsset(scanId);

			byte[] originalBytes = storageReader.readBytes(originalAsset.getStorageKey());
			String filename = OCR_FILENAME_PREFIX + scanId + OCR_FILENAME_SUFFIX;

			OcrResult ocrResult = ocrClient.recognize(originalBytes, filename);

			// 외부 호출 후 저장 직전 락 재확인
			if (!lockGuard.isOwned(scanId, lockOwner, lockToken)) return;

			persister.persistOcrSucceeded(scanId, lockOwner, lockToken, ocrResult, LocalDateTime.now());
			log.info("OCR 처리 완료 scanId={} 상태=OCR_DONE", scanId);

		} catch (Exception exception) {
			FailureDecision decision = failureDecider.decide(exception);
			persister.persistOcrFailed(scanId, lockOwner, lockToken, decision, LocalDateTime.now());

			if (exception instanceof RestClientResponseException restClientResponseException
				&& logPolicy.shouldSuppressStacktrace(restClientResponseException)) {
				log.warn("OCR 호출 4xx scanId={} reason={} status={}",
					scanId, logPolicy.reasonCode(decision), restClientResponseException.getRawStatusCode()
				);
			} else {
				log.error("OCR 처리 실패 scanId={} reason={}", scanId, logPolicy.reasonCode(decision), exception);
			}
		} finally {
			unlocker.unlockBestEffort(scanId, lockOwner, lockToken);
		}
	}
}
