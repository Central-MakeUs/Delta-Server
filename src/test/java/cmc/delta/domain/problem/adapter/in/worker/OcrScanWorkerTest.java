package cmc.delta.domain.problem.adapter.in.worker;

import static cmc.delta.domain.problem.adapter.in.worker.support.WorkerFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.worker.properties.OcrWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.support.WorkerTestTx;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.OcrFailureDecider;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanLockGuard;
import cmc.delta.domain.problem.adapter.in.worker.support.lock.ScanUnlocker;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.BacklogLogger;
import cmc.delta.domain.problem.adapter.in.worker.support.logging.WorkerLogPolicy;
import cmc.delta.domain.problem.adapter.in.worker.support.persistence.OcrScanPersister;
import cmc.delta.domain.problem.adapter.in.worker.support.validation.OcrScanValidator;
import cmc.delta.domain.problem.application.port.out.ocr.OcrClient;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.application.port.out.storage.ObjectStorageReader;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.global.storage.port.out.StoredObjectStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionTemplate;

class OcrScanWorkerTest {

	private static final String OWNER = "w1";
	private static final String TOKEN = "t1";

	private ObjectStorageReader storageReader;
	private OcrClient ocrClient;
	private OcrScanValidator validator;
	private OcrScanPersister persister;

	private ScanLockGuard lockGuard;
	private ScanUnlocker unlocker;

	private OcrFailureDecider failureDecider;

	private TestableOcrScanWorker sut;

	@BeforeEach
	void setUp() {
		TransactionTemplate tx = WorkerTestTx.immediateTx();
		Executor direct = Runnable::run;

		storageReader = mock(ObjectStorageReader.class);
		ocrClient = mock(OcrClient.class);
		validator = mock(OcrScanValidator.class);
		persister = mock(OcrScanPersister.class);

		lockGuard = mock(ScanLockGuard.class);
		unlocker = mock(ScanUnlocker.class);

		failureDecider = new OcrFailureDecider();

		sut = new TestableOcrScanWorker(
			Clock.systemDefaultZone(),
			tx,
			direct,
			mock(cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository.class),
			storageReader,
			ocrClient,
			new OcrWorkerProperties(2000L, 10, 30L, 1, 1),
			new cmc.delta.domain.problem.adapter.in.worker.support.ocr.LineDataSignalExtractor(
				new com.fasterxml.jackson.databind.ObjectMapper()),
			lockGuard,
			unlocker,
			mock(BacklogLogger.class),
			mock(WorkerLogPolicy.class),
			failureDecider,
			validator,
			persister);
	}

	@Test
	@DisplayName("OCR line_data에 code/pseudocode가 있으면 OCR_NOT_MATH로 실패 저장된다")
	void codeLine_failsWithNotMath() {
		// given
		Long scanId = 3L;
		LocalDateTime batchNow = LocalDateTime.of(2026, 1, 21, 10, 0);

		when(lockGuard.isOwned(scanId, OWNER, TOKEN)).thenReturn(true, true);

		Asset asset = asset("s3/key-3");
		when(validator.requireOriginalAsset(scanId)).thenReturn(asset);
		byte[] bytes = new byte[] {9};
		when(storageReader.openStream("s3/key-3"))
			.thenReturn(new StoredObjectStream(new ByteArrayInputStream(bytes), bytes.length));

		String rawJson = "{\"text\":\"import x;\",\"line_data\":[{\"type\":\"code\"}]}";
		OcrResult result = ocrResult("import x;", rawJson);
		when(ocrClient.recognize(any(InputStream.class), anyLong(), anyString())).thenReturn(result);

		// when
		sut.processOnePublic(scanId, OWNER, TOKEN, batchNow);

		// then
		ArgumentCaptor<FailureDecision> decisionCaptor = ArgumentCaptor.forClass(FailureDecision.class);
		verify(persister).persistOcrFailed(eq(scanId), eq(OWNER), eq(TOKEN), decisionCaptor.capture(), eq(batchNow));
		FailureDecision decision = decisionCaptor.getValue();
		assertThat(decision.reasonCode()).isEqualTo(FailureReason.OCR_NOT_MATH);
		assertThat(decision.retryable()).isFalse();
		verify(unlocker).unlockBestEffort(scanId, OWNER, TOKEN);
		verify(persister, never()).persistOcrSucceeded(anyLong(), anyString(), anyString(), any(), any());
	}

	@DisplayName("성공: validator→storageReader→ocrClient→persistOcrSucceeded→unlock 순으로 수행된다")
	void success_persistsAndUnlocks() {
		// given
		Long scanId = 7L;
		LocalDateTime batchNow = LocalDateTime.of(2026, 1, 21, 10, 0);

		when(lockGuard.isOwned(scanId, OWNER, TOKEN)).thenReturn(true, true);

		Asset asset = asset("s3/key-7");
		when(validator.requireOriginalAsset(scanId)).thenReturn(asset);
		byte[] bytes = new byte[] {9};
		when(storageReader.openStream("s3/key-7"))
			.thenReturn(new StoredObjectStream(new ByteArrayInputStream(bytes), bytes.length));

		OcrResult result = ocrResult("plain", "{\"ok\":true}");
		when(ocrClient.recognize(any(InputStream.class), anyLong(), anyString())).thenReturn(result);

		// when
		sut.processOnePublic(scanId, OWNER, TOKEN, batchNow);

		// then
		InOrder inOrder = inOrder(validator, storageReader, ocrClient, persister, unlocker);

		inOrder.verify(validator).requireOriginalAsset(scanId);
		inOrder.verify(storageReader).openStream("s3/key-7");
		inOrder.verify(ocrClient).recognize(any(InputStream.class), eq(1L), eq("scan-7.jpg"));
		inOrder.verify(persister).persistOcrSucceeded(scanId, OWNER, TOKEN, result, batchNow);
		inOrder.verify(unlocker).unlockBestEffort(scanId, OWNER, TOKEN);

		verify(persister, never()).persistOcrFailed(anyLong(), anyString(), anyString(), any(FailureDecision.class),
			any());
	}

	@Test
	@DisplayName("외부 호출 후 락을 잃으면 저장은 하지 않지만 unlock은 수행한다")
	void lockLost_afterExternalCall_skipsPersist_butUnlocks() {
		// given
		Long scanId = 1L;
		LocalDateTime batchNow = LocalDateTime.of(2026, 1, 21, 10, 0);

		when(lockGuard.isOwned(scanId, OWNER, TOKEN)).thenReturn(true, false);

		Asset asset = asset("s3/key");
		when(asset.getStorageKey()).thenReturn("s3/key");
		when(validator.requireOriginalAsset(scanId)).thenReturn(asset);

		byte[] bytes = new byte[] {1, 2};
		when(storageReader.openStream("s3/key"))
			.thenReturn(new StoredObjectStream(new ByteArrayInputStream(bytes), bytes.length));

		OcrResult result = mock(OcrResult.class);
		when(ocrClient.recognize(any(InputStream.class), eq(2L), anyString())).thenReturn(result);

		// when
		sut.processOnePublic(scanId, OWNER, TOKEN, batchNow);

		// then
		verify(persister, never()).persistOcrSucceeded(anyLong(), anyString(), anyString(), any(), any());
		verify(persister, never()).persistOcrFailed(anyLong(), anyString(), anyString(), any(), any());
		verify(unlocker).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	/**
	 * 테스트에서만 protected processOne을 호출하기 위한 래퍼.
	 */
	static final class TestableOcrScanWorker extends OcrScanWorker {
		TestableOcrScanWorker(
			Clock clock,
			TransactionTemplate workerTxTemplate,
			Executor ocrExecutor,
			cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository scanWorkRepository,
			ObjectStorageReader storageReader,
			OcrClient ocrClient,
			OcrWorkerProperties properties,
			cmc.delta.domain.problem.adapter.in.worker.support.ocr.LineDataSignalExtractor signalExtractor,
			ScanLockGuard lockGuard,
			ScanUnlocker unlocker,
			BacklogLogger backlogLogger,
			WorkerLogPolicy logPolicy,
			OcrFailureDecider failureDecider,
			OcrScanValidator validator,
			OcrScanPersister persister) {
			super(
				clock,
				workerTxTemplate,
				ocrExecutor,
				scanWorkRepository,
				storageReader,
				ocrClient,
				properties,
				signalExtractor,
				lockGuard,
				unlocker,
				backlogLogger,
				logPolicy,
				failureDecider,
				validator,
				persister);
		}

		void processOnePublic(Long scanId, String lockOwner, String lockToken, LocalDateTime batchNow) {
			super.processOne(scanId, lockOwner, lockToken, batchNow);
		}
	}
}
