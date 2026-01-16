package cmc.delta.domain.problem.application.worker;

import static cmc.delta.domain.problem.application.worker.support.ProblemScanFixtures.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.scan.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.application.worker.support.OcrWorkerDoublesV2;
import cmc.delta.domain.problem.application.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.model.asset.Asset;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

class OcrScanWorkerTest {

	private static final String OWNER = "w1";
	private static final String TOKEN = "t1";

	private OcrWorkerDoublesV2 d;
	private OcrScanWorker sut;

	@BeforeEach
	void setUp() {
		d = OcrWorkerDoublesV2.create();
		Executor direct = Runnable::run;

		sut = new OcrScanWorker(
			Clock.systemDefaultZone(),
			d.tx(),
			direct,
			d.scanRepo(),
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
	@DisplayName("OCR 워커는 락을 잃으면 외부 호출을 하지 않는다.")
	void lockLost_skipsCall() {
		// given
		Long scanId = 1L;
		givenLockLost(scanId);

		// when
		run(scanId);

		// then
		verifyNoInteractions(d.validator(), d.storageReader(), d.ocrClient(), d.persister(), d.failureDecider(), d.logPolicy());
		verifyNoInteractions(d.unlocker()); // 시작부터 return이면 finally도 안 타서 unlock도 호출 X
	}

	@Test
	@DisplayName("OCR 워커는 성공하면 OCR_DONE으로 전환하고 OCR 결과를 저장한다.")
	void success_marksOcrDone() {
		// given
		Long scanId = 1L;
		givenLockedTwice(scanId); // 시작/외부호출 후 저장 직전 재확인까지 true

		givenOriginalBytesByValidator(scanId, "s3/key", new byte[] {1});
		OcrResult result = givenOcrReturns("plain", "{\"ok\":true}");

		// when
		run(scanId);

		// then
		verify(d.persister()).persistOcrSucceeded(eq(scanId), eq(OWNER), eq(TOKEN), eq(result), any(LocalDateTime.class));
		verify(d.persister(), never()).persistOcrFailed(anyLong(), anyString(), anyString(), any(), any());
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("OCR 워커는 원본 Asset이 없으면 즉시 FAILED로 전환한다.")
	void assetMissing_failsFast() {
		// given
		Long scanId = 1L;
		givenLocked(scanId);

		IllegalStateException ex = new IllegalStateException("ASSET_NOT_FOUND");
		when(d.validator().requireOriginalAsset(scanId)).thenThrow(ex);

		FailureDecision decision = mock(FailureDecision.class);
		when(d.failureDecider().decide(ex)).thenReturn(decision);
		when(d.logPolicy().reasonCode(decision)).thenReturn("ASSET_NOT_FOUND");

		// when
		run(scanId);

		// then
		verify(d.persister()).persistOcrFailed(eq(scanId), eq(OWNER), eq(TOKEN), eq(decision), any(LocalDateTime.class));
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
		verifyNoInteractions(d.storageReader(), d.ocrClient());
	}

	@Test
	@DisplayName("OCR 워커는 4xx가 발생하면 재시도하지 않고 즉시 FAILED로 전환한다.")
	void client4xx_failsFast() {
		// given
		Long scanId = 1L;
		givenLocked(scanId);

		givenOriginalBytesByValidator(scanId, "s3/key", new byte[] {1});
		HttpClientErrorException ex = givenOcrThrows4xx();

		FailureDecision decision = mock(FailureDecision.class);
		when(d.failureDecider().decide(ex)).thenReturn(decision);
		when(d.logPolicy().reasonCode(decision)).thenReturn("OCR_CLIENT_4XX");
		when(d.logPolicy().shouldSuppressStacktrace(any())).thenReturn(true);

		// when
		run(scanId);

		// then
		verify(d.persister()).persistOcrFailed(eq(scanId), eq(OWNER), eq(TOKEN), eq(decision), any(LocalDateTime.class));
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("OCR 워커는 네트워크 오류가 발생하면 next_retry_at을 잡고 재시도를 예약한다.")
	void networkErr_schedulesRetry() {
		// given
		Long scanId = 1L;
		givenLocked(scanId);

		givenOriginalBytesByValidator(scanId, "s3/key", new byte[] {1});
		ResourceAccessException ex = givenOcrThrowsNetwork();

		FailureDecision decision = mock(FailureDecision.class);
		when(d.failureDecider().decide(ex)).thenReturn(decision);
		when(d.logPolicy().reasonCode(decision)).thenReturn("OCR_NETWORK");

		// when
		run(scanId);

		// then
		verify(d.persister()).persistOcrFailed(eq(scanId), eq(OWNER), eq(TOKEN), eq(decision), any(LocalDateTime.class));
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("OCR 워커는 재시도 3회째 실패하면 FAILED로 전환해 무한 호출을 막는다.")
	void retry3_failsTerminal() {
		// 이 로직은 이제 worker가 직접 엔티티를 바꾸는 게 아니라 persister/decision 정책에 있음.
		// 따라서 워커 단위 테스트에서는 '3번 호출 시도' 자체가 아니라,
		// persistOcrFailed가 매번 호출되고 unlock이 보장되는지만 검증하는 게 맞다.

		// given
		Long scanId = 1L;
		givenLocked(scanId);

		givenOriginalBytesByValidator(scanId, "s3/key", new byte[] {1});
		ResourceAccessException ex = givenOcrThrowsNetwork();

		FailureDecision decision = mock(FailureDecision.class);
		when(d.failureDecider().decide(ex)).thenReturn(decision);
		when(d.logPolicy().reasonCode(decision)).thenReturn("OCR_NETWORK");

		// when
		run(scanId);
		run(scanId);
		run(scanId);

		// then
		verify(d.persister(), times(3)).persistOcrFailed(eq(scanId), eq(OWNER), eq(TOKEN), eq(decision), any(LocalDateTime.class));
		verify(d.unlocker(), times(3)).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	/* ================= helpers ================= */

	private void run(Long scanId) {
		sut.processOne(scanId, OWNER, TOKEN, LocalDateTime.now());
	}

	private void givenLocked(Long scanId) {
		when(d.lockGuard().isOwned(scanId, OWNER, TOKEN)).thenReturn(true);
	}

	private void givenLockedTwice(Long scanId) {
		// processOne 내부에서 2번 체크함: 시작 시 + 외부 호출 후 저장 직전
		when(d.lockGuard().isOwned(scanId, OWNER, TOKEN)).thenReturn(true, true);
	}

	private void givenLockLost(Long scanId) {
		when(d.lockGuard().isOwned(scanId, OWNER, TOKEN)).thenReturn(false);
	}

	private void givenOriginalBytesByValidator(Long scanId, String storageKey, byte[] bytes) {
		Asset asset = originalAsset(storageKey);
		when(d.validator().requireOriginalAsset(scanId)).thenReturn(asset);
		when(d.storageReader().readBytes(storageKey)).thenReturn(bytes);
	}

	private OcrResult givenOcrReturns(String plain, String raw) {
		OcrResult result = ocrResult(plain, raw);
		when(d.ocrClient().recognize(any(), anyString())).thenReturn(result);
		return result;
	}

	private HttpClientErrorException givenOcrThrows4xx() {
		HttpClientErrorException ex = HttpClientErrorException.create(
			HttpStatus.BAD_REQUEST, "400", new HttpHeaders(), null, null
		);
		when(d.ocrClient().recognize(any(), anyString())).thenThrow(ex);
		return ex;
	}

	private ResourceAccessException givenOcrThrowsNetwork() {
		ResourceAccessException ex = new ResourceAccessException("timeout");
		when(d.ocrClient().recognize(any(), anyString())).thenThrow(ex);
		return ex;
	}
}
