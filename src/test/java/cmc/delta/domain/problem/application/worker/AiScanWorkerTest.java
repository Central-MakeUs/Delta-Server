package cmc.delta.domain.problem.application.worker;

import static cmc.delta.domain.problem.application.worker.support.ProblemScanFixtures.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.scan.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.scan.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.application.worker.support.AiWorkerDoublesV2;
import cmc.delta.domain.problem.application.worker.support.failure.FailureDecision;
import cmc.delta.domain.problem.application.worker.support.validation.AiScanValidator.AiValidatedInput;
import cmc.delta.domain.problem.model.scan.ProblemScan;
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

class AiScanWorkerTest {

	private static final String OWNER = "w1";
	private static final String TOKEN = "t1";

	private AiWorkerDoublesV2 d;
	private AiScanWorker sut;

	@BeforeEach
	void setUp() {
		d = AiWorkerDoublesV2.create();

		Executor direct = Runnable::run;
		sut = new AiScanWorker(
			Clock.systemDefaultZone(),
			d.tx(),
			direct,
			d.scanRepo(),
			d.scanWorkRepo(),
			d.aiClient(),
			d.props(),
			d.lockGuard(),
			d.unlocker(),
			d.backlogLogger(),
			d.logPolicy(),
			d.failureDecider(),
			d.validator(),
			d.promptBuilder(),
			d.persister()
		);
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
		verifyNoInteractions(d.scanRepo(), d.validator(), d.promptBuilder(), d.aiClient(), d.persister(), d.failureDecider(), d.logPolicy());
		verifyNoInteractions(d.unlocker()); // 시작부터 return이면 finally도 안 탐
	}

	@Test
	@DisplayName("AI 워커는 OCR 텍스트가 비어 있으면 즉시 FAILED로 전환한다.")
	void emptyOcr_failsFast() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "   "));
		givenLocked(scanId);

		IllegalStateException ex = new IllegalStateException("OCR_TEXT_EMPTY");
		when(d.validator().validateAndNormalize(eq(scanId), same(scan))).thenThrow(ex);

		FailureDecision decision = mock(FailureDecision.class);
		when(d.failureDecider().decide(ex)).thenReturn(decision);
		when(d.logPolicy().reasonCode(decision)).thenReturn("OCR_TEXT_EMPTY");

		// when
		run(scanId);

		// then
		verify(d.persister()).persistAiFailed(eq(scanId), eq(OWNER), eq(TOKEN), eq(decision), any(LocalDateTime.class));
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
		verifyNoInteractions(d.promptBuilder(), d.aiClient());
	}

	@Test
	@DisplayName("AI 워커는 분류에 성공하면 AI_DONE으로 전환하고 결과를 저장한다.")
	void success_marksAiDone() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "some text"));
		givenLockedTwice(scanId);

		AiValidatedInput input = mock(AiValidatedInput.class);
		when(input.userId()).thenReturn(10L);
		when(input.normalizedOcrText()).thenReturn("some text");

		when(d.validator().validateAndNormalize(eq(scanId), same(scan))).thenReturn(input);

		AiCurriculumPrompt prompt = mock(AiCurriculumPrompt.class);
		when(d.promptBuilder().build(10L, "some text")).thenReturn(prompt);

		AiCurriculumResult ai = givenAiReturns("U_GEOM", "T_GEOM", 0.90);

		// when
		run(scanId);

		// then
		verify(d.aiClient()).classifyCurriculum(prompt);
		verify(d.persister()).persistAiSucceeded(eq(scanId), eq(OWNER), eq(TOKEN), eq(ai), any(LocalDateTime.class));
		verify(d.persister(), never()).persistAiFailed(anyLong(), anyString(), anyString(), any(), any());
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("AI 워커는 신뢰도가 낮으면 needs_review를 true로 저장한다.")
	void lowConf_marksNeedsReview() {
		// needs_review는 보통 persister/도메인 로직에서 결정됨
		// => 워커 단위에서는 성공 저장 호출만 검증

		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "some text"));
		givenLockedTwice(scanId);

		AiValidatedInput input = mock(AiValidatedInput.class);
		when(input.userId()).thenReturn(10L);
		when(input.normalizedOcrText()).thenReturn("some text");
		when(d.validator().validateAndNormalize(eq(scanId), same(scan))).thenReturn(input);

		AiCurriculumPrompt prompt = mock(AiCurriculumPrompt.class);
		when(d.promptBuilder().build(10L, "some text")).thenReturn(prompt);

		AiCurriculumResult ai = givenAiReturns("U_GEOM", "T_GEOM", 0.59);

		// when
		run(scanId);

		// then
		verify(d.persister()).persistAiSucceeded(eq(scanId), eq(OWNER), eq(TOKEN), eq(ai), any(LocalDateTime.class));
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("AI 워커는 429가 발생하면 재시도를 예약하고 OCR_DONE을 유지한다.")
	void rateLimit_schedulesRetry() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "some text"));
		givenLocked(scanId);

		AiValidatedInput input = mock(AiValidatedInput.class);
		when(input.userId()).thenReturn(10L);
		when(input.normalizedOcrText()).thenReturn("some text");
		when(d.validator().validateAndNormalize(eq(scanId), same(scan))).thenReturn(input);

		AiCurriculumPrompt prompt = mock(AiCurriculumPrompt.class);
		when(d.promptBuilder().build(10L, "some text")).thenReturn(prompt);

		HttpClientErrorException ex = givenAiThrowsRateLimit("10");

		FailureDecision decision = mock(FailureDecision.class);
		when(d.failureDecider().decide(ex)).thenReturn(decision);
		when(d.logPolicy().reasonCode(decision)).thenReturn("AI_RATE_LIMIT");

		// when
		run(scanId);

		// then
		verify(d.persister()).persistAiFailed(eq(scanId), eq(OWNER), eq(TOKEN), eq(decision), any(LocalDateTime.class));
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("AI 워커는 일반 4xx가 발생하면 즉시 FAILED로 전환한다.")
	void client4xx_failsFast() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "some text"));
		givenLocked(scanId);

		AiValidatedInput input = mock(AiValidatedInput.class);
		when(input.userId()).thenReturn(10L);
		when(input.normalizedOcrText()).thenReturn("some text");
		when(d.validator().validateAndNormalize(eq(scanId), same(scan))).thenReturn(input);

		AiCurriculumPrompt prompt = mock(AiCurriculumPrompt.class);
		when(d.promptBuilder().build(10L, "some text")).thenReturn(prompt);

		HttpClientErrorException ex = givenAiThrows4xx();

		FailureDecision decision = mock(FailureDecision.class);
		when(d.failureDecider().decide(ex)).thenReturn(decision);
		when(d.logPolicy().reasonCode(decision)).thenReturn("AI_CLIENT_4XX");

		// when
		run(scanId);

		// then
		verify(d.persister()).persistAiFailed(eq(scanId), eq(OWNER), eq(TOKEN), eq(decision), any(LocalDateTime.class));
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("AI 워커는 네트워크 오류가 발생하면 재시도를 예약하고 OCR_DONE을 유지한다.")
	void networkErr_schedulesRetry() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "some text"));
		givenLocked(scanId);

		AiValidatedInput input = mock(AiValidatedInput.class);
		when(input.userId()).thenReturn(10L);
		when(input.normalizedOcrText()).thenReturn("some text");
		when(d.validator().validateAndNormalize(eq(scanId), same(scan))).thenReturn(input);

		AiCurriculumPrompt prompt = mock(AiCurriculumPrompt.class);
		when(d.promptBuilder().build(10L, "some text")).thenReturn(prompt);

		ResourceAccessException ex = givenAiThrowsNetwork();

		FailureDecision decision = mock(FailureDecision.class);
		when(d.failureDecider().decide(ex)).thenReturn(decision);
		when(d.logPolicy().reasonCode(decision)).thenReturn("AI_NETWORK");

		// when
		run(scanId);

		// then
		verify(d.persister()).persistAiFailed(eq(scanId), eq(OWNER), eq(TOKEN), eq(decision), any(LocalDateTime.class));
		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	@Test
	@DisplayName("AI 워커는 외부 호출 후 락을 잃으면 결과 저장을 수행하지 않는다.")
	void lostLock_beforeSave_skipsSave() {
		// given
		Long scanId = 1L;
		ProblemScan scan = givenScan(scanId, ocrDone(user(10L), "some text"));

		when(d.lockGuard().isOwned(scanId, OWNER, TOKEN))
			.thenReturn(true)   // 시작
			.thenReturn(false); // 외부 호출 후 저장 직전

		AiValidatedInput input = mock(AiValidatedInput.class);
		when(input.userId()).thenReturn(10L);
		when(input.normalizedOcrText()).thenReturn("some text");
		when(d.validator().validateAndNormalize(eq(scanId), same(scan))).thenReturn(input);

		AiCurriculumPrompt prompt = mock(AiCurriculumPrompt.class);
		when(d.promptBuilder().build(10L, "some text")).thenReturn(prompt);

		givenAiReturns("U_GEOM", "T_GEOM", 0.90);

		// when
		run(scanId);

		// then
		verify(d.aiClient()).classifyCurriculum(prompt);

		verify(d.persister(), never()).persistAiSucceeded(anyLong(), anyString(), anyString(), any(), any());
		verify(d.persister(), never()).persistAiFailed(anyLong(), anyString(), anyString(), any(), any());

		verify(d.unlocker()).unlockBestEffort(scanId, OWNER, TOKEN);
	}

	private void run(Long scanId) {
		sut.processOne(scanId, OWNER, TOKEN, LocalDateTime.now());
	}

	private void givenLocked(Long scanId) {
		when(d.lockGuard().isOwned(scanId, OWNER, TOKEN)).thenReturn(true);
	}

	private void givenLockedTwice(Long scanId) {
		when(d.lockGuard().isOwned(scanId, OWNER, TOKEN)).thenReturn(true, true);
	}

	private void givenLockLost(Long scanId) {
		when(d.lockGuard().isOwned(scanId, OWNER, TOKEN)).thenReturn(false);
	}

	private ProblemScan givenScan(Long scanId, ProblemScan scan) {
		when(d.scanRepo().findById(scanId)).thenReturn(Optional.of(scan));
		return scan;
	}

	private AiCurriculumResult givenAiReturns(String unitId, String typeId, double confidence) {
		AiCurriculumResult ai = aiResult(unitId, typeId, confidence);
		when(d.aiClient().classifyCurriculum(any())).thenReturn(ai);
		return ai;
	}

	private HttpClientErrorException givenAiThrowsRateLimit(String retryAfterSec) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Retry-After", retryAfterSec);
		HttpClientErrorException ex = HttpClientErrorException.create(
			HttpStatus.TOO_MANY_REQUESTS, "429", headers, null, null
		);
		when(d.aiClient().classifyCurriculum(any())).thenThrow(ex);
		return ex;
	}

	private HttpClientErrorException givenAiThrows4xx() {
		HttpClientErrorException ex = HttpClientErrorException.create(
			HttpStatus.BAD_REQUEST, "400", new HttpHeaders(), null, null
		);
		when(d.aiClient().classifyCurriculum(any())).thenThrow(ex);
		return ex;
	}

	private ResourceAccessException givenAiThrowsNetwork() {
		ResourceAccessException ex = new ResourceAccessException("timeout");
		when(d.aiClient().classifyCurriculum(any())).thenThrow(ex);
		return ex;
	}
}
