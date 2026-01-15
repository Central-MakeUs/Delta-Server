package cmc.delta.domain.problem.application.worker;

import static cmc.delta.domain.problem.application.worker.support.ProblemScanFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.port.ai.AiCurriculumResult;
import cmc.delta.domain.problem.application.worker.support.AiWorkerDoubles;
import cmc.delta.domain.problem.model.ProblemScan;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

class AiScanWorkerTest {

	private static final String OWNER = "w1";
	private static final String TOKEN = "t1";

	private AiWorkerDoubles d;
	private AiScanWorker sut;

	@BeforeEach
	void setUp() {
		d = AiWorkerDoubles.create();

		Executor direct = Runnable::run;
		sut = new AiScanWorker(
			Clock.systemDefaultZone(),
			d.tx(),
			direct,
			d.scanRepo(),
			d.unitRepo(),
			d.typeRepo(),
			d.aiClient()
		);

		// buildPrompt에서 옵션 조회하므로 기본값(빈 리스트)로 고정
		when(d.unitRepo().findAllRootUnitsActive()).thenReturn(List.of());
		when(d.unitRepo().findAllChildUnitsActive()).thenReturn(List.of());
		when(d.typeRepo().findAllActiveForUser(anyLong())).thenReturn(List.of());
	}

	@Test
	@DisplayName("AI 워커는 락을 잃으면 외부 호출을 하지 않는다.")
	void lockLost_skipsCall() {
		// given
		Long scanId = 1L;
		givenLockLost(scanId);

		// when
		run(scanId);

		// then
		verifyNoInteractions(d.aiClient());
	}

	@Test
	@DisplayName("AI 워커는 OCR 텍스트가 비어 있으면 즉시 FAILED로 전환한다.")
	void emptyOcr_failsFast() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "   "));
		givenLocked(scanId);

		// when
		run(scanId);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
		assertThat(scan.getFailReason()).isEqualTo("OCR_TEXT_EMPTY");
	}

	@Test
	@DisplayName("AI 워커는 분류에 성공하면 AI_DONE으로 전환하고 결과를 저장한다.")
	void success_marksAiDone() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "some text"));
		givenLocked(scanId);
		givenAiReturns("U_GEOM", "T_GEOM", 0.90);
		givenUnitTypeExists("U_GEOM", "T_GEOM");

		// when
		run(scanId);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.AI_DONE);
		assertThat(scan.isNeedsReview()).isFalse();
	}

	@Test
	@DisplayName("AI 워커는 신뢰도가 낮으면 needs_review를 true로 저장한다.")
	void lowConf_marksNeedsReview() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "some text"));
		givenLocked(scanId);
		givenAiReturns("U_GEOM", "T_GEOM", 0.59);
		givenUnitTypeExists("U_GEOM", "T_GEOM");

		// when
		run(scanId);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.AI_DONE);
		assertThat(scan.isNeedsReview()).isTrue();
	}

	@Test
	@DisplayName("AI 워커는 429가 발생하면 재시도를 예약하고 OCR_DONE을 유지한다.")
	void rateLimit_schedulesRetry() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "some text"));
		givenLocked(scanId);
		givenAiThrowsRateLimit("10");

		// when
		run(scanId);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.OCR_DONE);
		assertThat(scan.getAiAttemptCount()).isEqualTo(1);
		assertThat(scan.getFailReason()).isEqualTo("AI_RATE_LIMIT");
		assertThat(scan.getNextRetryAt()).isNotNull();
	}

	@Test
	@DisplayName("AI 워커는 일반 4xx가 발생하면 즉시 FAILED로 전환한다.")
	void client4xx_failsFast() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "some text"));
		givenLocked(scanId);
		givenAiThrows4xx();

		// when
		run(scanId);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.FAILED);
		assertThat(scan.getFailReason()).isEqualTo("AI_CLIENT_4XX");
	}

	@Test
	@DisplayName("AI 워커는 네트워크 오류가 발생하면 재시도를 예약하고 OCR_DONE을 유지한다.")
	void networkErr_schedulesRetry() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "some text"));
		givenLocked(scanId);
		when(d.aiClient().classifyCurriculum(any()))
			.thenThrow(new ResourceAccessException("timeout"));

		// when
		run(scanId);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.OCR_DONE);
		assertThat(scan.getAiAttemptCount()).isEqualTo(1);
		assertThat(scan.getNextRetryAt()).isNotNull();
	}

	@Test
	@DisplayName("AI 워커는 외부 호출 후 락을 잃으면 결과 저장을 수행하지 않는다.")
	void lostLock_beforeSave_skipsSave() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "some text"));

		when(d.scanRepo().existsLockedBy(scanId, OWNER, TOKEN))
			.thenReturn(1)     // 시작 시점: 내 락
			.thenReturn(null); // 외부 호출 후: 락 steal

		givenAiReturns("U_GEOM", "T_GEOM", 0.90);

		// when
		run(scanId);

		// then
		assertThat(scan.getStatus()).isEqualTo(ScanStatus.OCR_DONE);

		// prompt 옵션 조회는 발생할 수 있으니 허용하고,
		verify(d.unitRepo(), never()).findById(anyString());
		verify(d.typeRepo(), never()).findById(anyString());
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

	private void givenAiReturns(String unitId, String typeId, double confidence) {
		AiCurriculumResult ai = aiResult(unitId, typeId, confidence);
		when(d.aiClient().classifyCurriculum(any())).thenReturn(ai);
	}

	private void givenUnitTypeExists(String unitId, String typeId) {
		Unit u = unit(unitId);
		ProblemType t = type(typeId);

		when(d.unitRepo().findById(unitId)).thenReturn(Optional.of(u));
		when(d.typeRepo().findById(typeId)).thenReturn(Optional.of(t));
	}

	private void givenAiThrowsRateLimit(String retryAfterSec) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Retry-After", retryAfterSec);
		HttpClientErrorException ex = HttpClientErrorException.create(
			HttpStatus.TOO_MANY_REQUESTS, "429", headers, null, null
		);
		when(d.aiClient().classifyCurriculum(any())).thenThrow(ex);
	}

	private void givenAiThrows4xx() {
		HttpClientErrorException ex = HttpClientErrorException.create(
			HttpStatus.BAD_REQUEST, "400", new HttpHeaders(), null, null
		);
		when(d.aiClient().classifyCurriculum(any())).thenThrow(ex);
	}
}
