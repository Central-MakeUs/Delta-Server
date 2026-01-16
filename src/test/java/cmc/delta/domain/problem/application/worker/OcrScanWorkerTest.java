package cmc.delta.domain.problem.application.worker;

import static cmc.delta.domain.problem.application.worker.support.ProblemScanFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.scan.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.application.worker.support.OcrWorkerDoubles;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
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

	private OcrWorkerDoubles d;
	private OcrScanWorker sut;

	@BeforeEach
	void setUp() {
		d = OcrWorkerDoubles.create();
		Executor direct = Runnable::run;

		sut = new OcrScanWorker(
			Clock.systemDefaultZone(),
			d.tx(),
			direct,
			d.scanRepo(),
			d.assetRepo(),
			d.storageReader(),
			d.ocrClient(),
			d.props()
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
		verifyNoInteractions(d.assetRepo(), d.storageReader(), d.ocrClient());
	}

	@Test
	@DisplayName("OCR 워커는 성공하면 OCR_DONE으로 전환하고 OCR 결과를 저장한다.")
	void success_marksOcrDone() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, uploaded(user(10L)));
		givenLocked(scanId);
		givenOriginalBytes(scanId, "s3/key", new byte[] {1});
		givenOcrReturns("plain", "{\"ok\":true}");

		// when
		run(scanId);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.OCR_DONE);
		assertThat(scan.getOcrPlainText()).isEqualTo("plain");
		assertThat(scan.getOcrRawJson()).isEqualTo("{\"ok\":true}");
		assertThat(scan.getFailReason()).isNull();
		assertThat(scan.getNextRetryAt()).isNull();
	}

	@Test
	@DisplayName("OCR 워커는 원본 Asset이 없으면 즉시 FAILED로 전환한다.")
	void assetMissing_failsFast() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, uploaded(user(10L)));
		givenLocked(scanId);
		givenNoOriginalAsset(scanId);

		// when
		run(scanId);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
		assertThat(scan.getFailReason()).isEqualTo("ASSET_NOT_FOUND");
	}

	@Test
	@DisplayName("OCR 워커는 4xx가 발생하면 재시도하지 않고 즉시 FAILED로 전환한다.")
	void client4xx_failsFast() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, uploaded(user(10L)));
		givenLocked(scanId);
		givenOriginalBytes(scanId, "s3/key", new byte[] {1});
		givenOcrThrows4xx();

		// when
		run(scanId);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
		assertThat(scan.getFailReason()).isEqualTo("OCR_CLIENT_4XX");
	}

	@Test
	@DisplayName("OCR 워커는 네트워크 오류가 발생하면 next_retry_at을 잡고 재시도를 예약한다.")
	void networkErr_schedulesRetry() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, uploaded(user(10L)));
		givenLocked(scanId);
		givenOriginalBytes(scanId, "s3/key", new byte[] {1});
		givenOcrThrowsNetwork();

		// when
		run(scanId);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.UPLOADED);
		assertThat(scan.getOcrAttemptCount()).isEqualTo(1);
		assertThat(scan.getNextRetryAt()).isNotNull();
	}

	@Test
	@DisplayName("OCR 워커는 재시도 3회째 실패하면 FAILED로 전환해 무한 호출을 막는다.")
	void retry3_failsTerminal() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, uploaded(user(10L)));
		givenLocked(scanId);
		givenOriginalBytes(scanId, "s3/key", new byte[] {1});
		givenOcrThrowsNetwork();

		// when
		run(scanId);
		run(scanId);
		run(scanId);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
		assertThat(scan.getOcrAttemptCount()).isEqualTo(3);
		assertThat(scan.getNextRetryAt()).isNull();
	}

	private void run(Long scanId) {
		sut.processOne(scanId, OWNER, TOKEN, LocalDateTime.now());
	}

	private void givenLocked(Long scanId) {
		when(d.scanRepo().existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(1);
	}

	private void givenLockLost(Long scanId) {
		when(d.scanRepo().existsLockedBy(scanId, OWNER, TOKEN)).thenReturn(null);
	}

	private ProblemScan givenScan(Long scanId, ProblemScan scan) {
		when(d.scanRepo().findById(scanId)).thenReturn(Optional.of(scan));
		return scan;
	}

	private void givenOriginalBytes(Long scanId, String storageKey, byte[] bytes) {
		Asset asset = originalAsset(storageKey);

		when(d.assetRepo().findOriginalByScanId(scanId)).thenReturn(Optional.of(asset));
		when(d.storageReader().readBytes(storageKey)).thenReturn(bytes);
	}

	private void givenNoOriginalAsset(Long scanId) {
		when(d.assetRepo().findOriginalByScanId(scanId)).thenReturn(Optional.empty());
	}

	private void givenOcrReturns(String plain, String raw) {
		OcrResult result = ocrResult(plain, raw);
		when(d.ocrClient().recognize(any(), anyString())).thenReturn(result);
	}

	private void givenOcrThrows4xx() {
		HttpClientErrorException ex = HttpClientErrorException.create(
			HttpStatus.BAD_REQUEST, "400", new HttpHeaders(), null, null
		);
		when(d.ocrClient().recognize(any(), anyString())).thenThrow(ex);
	}

	private void givenOcrThrowsNetwork() {
		when(d.ocrClient().recognize(any(), anyString()))
			.thenThrow(new ResourceAccessException("timeout"));
	}
}
