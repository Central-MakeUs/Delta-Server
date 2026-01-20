package cmc.delta.domain.problem.application.worker.support;

import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.port.out.ocr.OcrClient;
import cmc.delta.domain.problem.application.port.out.storage.ObjectStorageReader;
import cmc.delta.domain.problem.adapter.in.worker.properties.OcrWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.OcrFailureDecider;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanUnlocker;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.BacklogLogger;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.WorkerLogPolicy;
import cmc.delta.domain.problem.adapter.in.worker.support.persistence.OcrScanPersister;
import cmc.delta.domain.problem.adapter.in.worker.support.validation.OcrScanValidator;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;

import org.springframework.transaction.support.TransactionTemplate;

public record OcrWorkerDoublesV2(
	ScanRepository scanRepo,
	ScanWorkRepository scanWorkRepo,
	ObjectStorageReader storageReader,
	OcrClient ocrClient,
	OcrWorkerProperties props,
	TransactionTemplate tx,
	ScanLockGuard lockGuard,
	ScanUnlocker unlocker,
	BacklogLogger backlogLogger,
	WorkerLogPolicy logPolicy,
	OcrFailureDecider failureDecider,
	OcrScanValidator validator,
	OcrScanPersister persister
) {
	public static OcrWorkerDoublesV2 create() {
		TransactionTemplate tx = WorkerTestTx.immediateTx();
		OcrWorkerProperties props = new OcrWorkerProperties(2000L, 10, 30L, 1, 1);

		return new OcrWorkerDoublesV2(
			mock(ScanRepository.class),
			mock(ScanWorkRepository.class),
			mock(ObjectStorageReader.class),
			mock(OcrClient.class),
			props,
			tx,
			mock(ScanLockGuard.class),
			mock(ScanUnlocker.class),
			mock(BacklogLogger.class),
			mock(WorkerLogPolicy.class),
			mock(OcrFailureDecider.class),
			mock(OcrScanValidator.class),
			mock(OcrScanPersister.class)
		);
	}
}
