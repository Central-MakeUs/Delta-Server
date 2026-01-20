package cmc.delta.domain.problem.application.worker;

import static cmc.delta.domain.problem.application.worker.support.ProblemScanFixtures.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.worker.OcrScanWorker;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.application.worker.support.OcrWorkerDoublesV2;
import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.model.asset.Asset;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

class OcrScanWorkerAdditionalTest {

	private static final String OWNER = "w1";
	private static final String TOKEN = "t1";

	private OcrWorkerDoublesV2 d;
	private OcrScanWorker sut;

	private Clock fixedClock;

	@BeforeEach
	void setUp() {
		d = OcrWorkerDoublesV2.create();
		Executor direct = Runnable::run;

		fixedClock = Clock.fixed(
			Instant.parse("2026-01-16T08:00:00Z"),
			ZoneId.systemDefault()
		);

		sut = new OcrScanWorker(
			fixedClock,
			d.tx(),
			direct,
			d.scanWorkRepo(),
			d.storageReader(),
			d.ocrClient(),
			d.props(),
			d.lockGuard(),
			d.unlocker(),
			d.backlogLogger(),
			d.logPolicy(),
			d.failureDecider(),
			d.validator(),
			d.persister()
		);

	}

	@Test
	@DisplayName("락이 없으면(초기) 아무 것도 하지 않고 즉시 종료한다 - 외부호출/저장/언락 모두 X")
	void lockLost_beforeStart_returnsImmediately() {
		// given
		Long scanId = 1L;
		givenLockOwned(scanId, false);

		// when
		run(scanId);

		// then
		verifyNoInteractions(d.validator(), d.storageReader(), d.ocrClient(), d.persister(), d.failureDecider(), d.logPolicy());
		verifyNoInteractions(d.unlocker()); // try/finally 진입 전 return이라 unlock도 안 함(의도대로)
	}

	@Test
	@DisplayName("외부 호출 후 락을 잃으면 결과 저장은 하지 않지만 finally에서 unlock은 best-effort로 수행한다")
	void lockLost_afterExternalCall_skipsPersist_butUnlocks() {
		// given
		Long scanId = 1L;
		givenLockOwnedTwice(scanId, true, false); // 시작은 owned, 외부 호출 후 owned=false
		givenOriginalAsset(scanId, "s3/key");
		givenOriginalBytes("s3/key", new byte[] {1, 2});
		givenOcrReturns("plain", "{\"ok\":true}");

		// when
		run(scanId);

		// then
		verify(d.ocrClient()).recognize(eq(new byte[] {1, 2}), eq("scan-1.jpg"));
		verify(d.persister(), never()).persistOcrSucceeded(anyLong(), anyString(), anyString(), any(), any());
		verify(d.persister(), never()).persistOcrFailed(anyLong(), anyString(), anyString(), any(), any());
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("성공 시: validator→storageReader→ocrClient 순으로 호출되고, persistOcrSucceeded 및 unlock이 수행된다")
	void success_persistsAndUnlocks() {
		// given
		Long scanId = 7L;
		givenLockOwnedTwice(scanId, true, true);
		givenOriginalAsset(scanId, "s3/key-7");
		givenOriginalBytes("s3/key-7", new byte[] {9});
		OcrResult result = givenOcrReturns("p", "{\"r\":1}");

		// when
		run(scanId);

		// then
		InOrder inOrder = inOrder(d.validator(), d.storageReader(), d.ocrClient(), d.persister(), d.unlocker());

		inOrder.verify(d.validator()).requireOriginalAsset(scanId);
		inOrder.verify(d.storageReader()).readBytes("s3/key-7");
		inOrder.verify(d.ocrClient()).recognize(eq(new byte[] {9}), eq("scan-7.jpg"));
		inOrder.verify(d.persister()).persistOcrSucceeded(eq(scanId), eq(OWNER), eq(TOKEN), eq(result), any(LocalDateTime.class));
		inOrder.verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);

		verify(d.persister(), never()).persistOcrFailed(anyLong(), anyString(), anyString(), any(), any());
	}

	@Test
	@DisplayName("validator(requireOriginalAsset)에서 예외가 나면 failureDecider→persistOcrFailed→unlock이 수행된다")
	void validatorThrows_persistFailedAndUnlocks() {
		// given
		Long scanId = 2L;
		givenLockOwned(scanId, true);

		RuntimeException ex = new IllegalStateException("ASSET_NOT_FOUND");
		when(d.validator().requireOriginalAsset(scanId)).thenThrow(ex);

		FailureDecision decision = mock(FailureDecision.class);
		when(d.failureDecider().decide(ex)).thenReturn(decision);
		when(d.logPolicy().reasonCode(decision)).thenReturn("ASSET_NOT_FOUND");

		// when
		run(scanId);

		// then
		verify(d.failureDecider()).decide(ex);
		verify(d.persister()).persistOcrFailed(eq(scanId), eq(OWNER), eq(TOKEN), eq(decision), any(LocalDateTime.class));
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);

		verifyNoInteractions(d.storageReader(), d.ocrClient());
	}

	@Test
	@DisplayName("OCR 4xx(RestClientResponseException)이면 logPolicy.suppress 판단 경로를 탄다(저장/언락은 수행)")
	void ocr4xx_usesSuppressPolicy_andPersistsFailed() {
		// given
		Long scanId = 3L;
		givenLockOwned(scanId, true);
		givenOriginalAsset(scanId, "s3/key");
		givenOriginalBytes("s3/key", new byte[] {1});

		HttpClientErrorException ex = HttpClientErrorException.create(
			HttpStatus.BAD_REQUEST, "400", new HttpHeaders(), null, null
		);
		when(d.ocrClient().recognize(any(), anyString())).thenThrow(ex);

		FailureDecision decision = mock(FailureDecision.class);
		when(d.failureDecider().decide(ex)).thenReturn(decision);

		when(d.logPolicy().shouldSuppressStacktrace(any(RestClientResponseException.class))).thenReturn(true);
		when(d.logPolicy().reasonCode(decision)).thenReturn("OCR_CLIENT_4XX");

		// when
		run(scanId);

		// then
		verify(d.failureDecider()).decide(ex);
		verify(d.logPolicy()).shouldSuppressStacktrace(ex);
		verify(d.persister()).persistOcrFailed(eq(scanId), eq(OWNER), eq(TOKEN), eq(decision), any(LocalDateTime.class));
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("네트워크 오류(ResourceAccessException)면 failureDecider→persistOcrFailed→unlock 수행")
	void networkError_persistsFailedAndUnlocks() {
		// given
		Long scanId = 4L;
		givenLockOwned(scanId, true);
		givenOriginalAsset(scanId, "s3/key");
		givenOriginalBytes("s3/key", new byte[] {1});

		ResourceAccessException ex = new ResourceAccessException("timeout");
		when(d.ocrClient().recognize(any(), anyString())).thenThrow(ex);

		FailureDecision decision = mock(FailureDecision.class);
		when(d.failureDecider().decide(ex)).thenReturn(decision);
		when(d.logPolicy().reasonCode(decision)).thenReturn("OCR_NETWORK");

		// when
		run(scanId);

		// then
		verify(d.failureDecider()).decide(ex);
		verify(d.persister()).persistOcrFailed(eq(scanId), eq(OWNER), eq(TOKEN), eq(decision), any(LocalDateTime.class));
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	private void run(Long scanId) {
		sut.processOne(scanId, OWNER, TOKEN, LocalDateTime.now(fixedClock));
	}

	private void givenLockOwned(Long scanId, boolean owned) {
		when(d.lockGuard().isOwned(scanId, OWNER, TOKEN)).thenReturn(owned);
	}

	private void givenLockOwnedTwice(Long scanId, boolean first, boolean second) {
		when(d.lockGuard().isOwned(scanId, OWNER, TOKEN)).thenReturn(first, second);
	}

	private void givenOriginalAsset(Long scanId, String storageKey) {
		Asset asset = originalAsset(storageKey);
		when(d.validator().requireOriginalAsset(scanId)).thenReturn(asset);
	}

	private void givenOriginalBytes(String storageKey, byte[] bytes) {
		when(d.storageReader().readBytes(storageKey)).thenReturn(bytes);
	}

	private OcrResult givenOcrReturns(String plain, String raw) {
		OcrResult result = ocrResult(plain, raw);
		when(d.ocrClient().recognize(any(), anyString())).thenReturn(result);
		return result;
	}
}
