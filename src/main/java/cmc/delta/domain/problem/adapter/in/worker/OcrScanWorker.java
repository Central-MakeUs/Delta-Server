package cmc.delta.domain.problem.adapter.in.worker;

import cmc.delta.domain.problem.adapter.in.worker.properties.OcrWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.support.AbstractExternalCallScanWorker;
import cmc.delta.domain.problem.adapter.in.worker.support.WorkerIdentity;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.OcrFailureDecider;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanUnlocker;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.BacklogLogger;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.WorkerLogPolicy;
import cmc.delta.domain.problem.adapter.in.worker.support.persistence.OcrScanPersister;
import cmc.delta.domain.problem.adapter.in.worker.support.validation.OcrScanValidator;
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import cmc.delta.domain.problem.application.port.out.ocr.OcrClient;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.application.port.out.storage.ObjectStorageReader;
import cmc.delta.domain.problem.model.asset.Asset;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class OcrScanWorker extends AbstractExternalCallScanWorker {

	private static final WorkerIdentity IDENTITY = new WorkerIdentity("ocr", "OCR", "worker:ocr:backlog");

	private static final String OCR_FILENAME_PREFIX = "scan-";
	private static final String OCR_FILENAME_SUFFIX = ".jpg";

	private final ScanWorkRepository scanWorkRepository;
	private final ObjectStorageReader storageReader;
	private final OcrClient ocrClient;
	private final OcrWorkerProperties properties;

	private final OcrFailureDecider failureDecider;
	private final OcrScanValidator validator;
	private final OcrScanPersister persister;

	public OcrScanWorker(
		Clock clock,
		TransactionTemplate workerTxTemplate,
		@Qualifier("ocrExecutor")
		Executor ocrExecutor,
		ScanWorkRepository scanWorkRepository,
		ObjectStorageReader storageReader,
		OcrClient ocrClient,
		OcrWorkerProperties properties,
		ScanLockGuard lockGuard,
		ScanUnlocker unlocker,
		BacklogLogger backlogLogger,
		WorkerLogPolicy logPolicy,
		OcrFailureDecider failureDecider,
		OcrScanValidator validator,
		OcrScanPersister persister) {
		super(clock, workerTxTemplate, ocrExecutor, IDENTITY, lockGuard, unlocker, backlogLogger, logPolicy);
		this.scanWorkRepository = scanWorkRepository;
		this.storageReader = storageReader;
		this.ocrClient = ocrClient;
		this.properties = properties;
		this.failureDecider = failureDecider;
		this.validator = validator;
		this.persister = persister;
	}

	@Override
	protected int claim(LocalDateTime now, LocalDateTime staleBefore, String lockOwner, String lockToken,
		LocalDateTime lockedAt, int limit) {
		return scanWorkRepository.claimOcrCandidates(now, staleBefore, lockOwner, lockToken, lockedAt, limit);
	}

	@Override
	protected List<Long> findClaimedIds(String lockOwner, String lockToken, int limit) {
		return scanWorkRepository.findClaimedOcrIds(lockOwner, lockToken, limit);
	}

	@Override
	protected long backlogLogMinutes() {
		return properties.backlogLogMinutes();
	}

	@Override
	protected long countBacklog(LocalDateTime now, LocalDateTime staleBefore) {
		return scanWorkRepository.countOcrBacklog(now, staleBefore);
	}

	@Override
	protected void handleSuccess(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
		Asset originalAsset = validator.requireOriginalAsset(scanId);

		byte[] originalBytes = storageReader.readBytes(originalAsset.getStorageKey());
		OcrResult ocrResult = ocrClient.recognize(originalBytes, buildFilename(scanId));

		if (!isOwned(scanId, lockOwner, lockToken)) {
			return;
		}

		persister.persistOcrSucceeded(scanId, lockOwner, lockToken, ocrResult, batchNow);
		log.info("OCR 처리 완료 scanId={} 상태=OCR_DONE", scanId);
	}

	@Override
	protected FailureDecision decideFailure(Exception exception) {
		return failureDecider.decide(exception);
	}

	@Override
	protected void persistFailed(Long scanId, String lockOwner, String lockToken, FailureDecision decision,
		LocalDateTime batchNow) {
		persister.persistOcrFailed(scanId, lockOwner, lockToken, decision, batchNow);
	}

	private String buildFilename(Long scanId) {
		return OCR_FILENAME_PREFIX + scanId + OCR_FILENAME_SUFFIX;
	}
}
