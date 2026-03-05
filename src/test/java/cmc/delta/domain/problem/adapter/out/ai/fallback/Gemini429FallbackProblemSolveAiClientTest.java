package cmc.delta.domain.problem.adapter.out.ai.fallback;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.out.ai.gemini.GeminiAiException;
import cmc.delta.domain.problem.adapter.out.ai.gemini.GeminiProblemSolveAiClient;
import cmc.delta.domain.problem.adapter.out.ai.openai.OpenAiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolvePrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolveResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

class Gemini429FallbackProblemSolveAiClientTest {

	private final GeminiProblemSolveAiClient geminiProblemSolveAiClient = mock(GeminiProblemSolveAiClient.class);
	private final OpenAiClient openAiClient = mock(OpenAiClient.class);
	private final Gemini429FallbackProblemSolveAiClient fallbackClient = new Gemini429FallbackProblemSolveAiClient(
		geminiProblemSolveAiClient,
		openAiClient);

	@Test
	@DisplayName("Gemini 풀이 성공이면 OpenAI fallback 없이 Gemini 결과를 사용한다")
	void solveProblem_whenGeminiSucceeded_thenReturnsGeminiResult() {
		ProblemAiSolvePrompt prompt = mock(ProblemAiSolvePrompt.class);
		ProblemAiSolveResult geminiResult = new ProblemAiSolveResult("latex", "text");
		when(geminiProblemSolveAiClient.solveProblem(prompt)).thenReturn(geminiResult);

		ProblemAiSolveResult actual = fallbackClient.solveProblem(prompt);

		assertThat(actual).isEqualTo(geminiResult);
		verifyNoInteractions(openAiClient);
	}

	@Test
	@DisplayName("Gemini 풀이 429 + OpenAI 활성화면 OpenAI로 1회 fallback 한다")
	void solveProblem_whenGemini429AndOpenAiEnabled_thenFallbackToOpenAi() {
		ProblemAiSolvePrompt prompt = mock(ProblemAiSolvePrompt.class);
		ProblemAiSolveResult openAiResult = new ProblemAiSolveResult("fallback-latex", "fallback-text");
		when(geminiProblemSolveAiClient.solveProblem(prompt)).thenThrow(rateLimitException());
		when(openAiClient.isEnabled()).thenReturn(true);
		when(openAiClient.solveProblem(prompt)).thenReturn(openAiResult);

		ProblemAiSolveResult actual = fallbackClient.solveProblem(prompt);

		assertThat(actual).isEqualTo(openAiResult);
		verify(openAiClient).solveProblem(prompt);
	}

	@Test
	@DisplayName("Gemini 풀이 503 + OpenAI 활성화면 OpenAI로 1회 fallback 한다")
	void solveProblem_whenGemini503AndOpenAiEnabled_thenFallbackToOpenAi() {
		ProblemAiSolvePrompt prompt = mock(ProblemAiSolvePrompt.class);
		ProblemAiSolveResult openAiResult = new ProblemAiSolveResult("fallback-latex", "fallback-text");
		when(geminiProblemSolveAiClient.solveProblem(prompt)).thenThrow(serverErrorException());
		when(openAiClient.isEnabled()).thenReturn(true);
		when(openAiClient.solveProblem(prompt)).thenReturn(openAiResult);

		ProblemAiSolveResult actual = fallbackClient.solveProblem(prompt);

		assertThat(actual).isEqualTo(openAiResult);
		verify(openAiClient).solveProblem(prompt);
	}

	@Test
	@DisplayName("Gemini 풀이 429 + OpenAI 비활성화면 Gemini 예외를 그대로 던진다")
	void solveProblem_whenGemini429AndOpenAiDisabled_thenThrowsGeminiException() {
		ProblemAiSolvePrompt prompt = mock(ProblemAiSolvePrompt.class);
		GeminiAiException exception = rateLimitException();
		when(geminiProblemSolveAiClient.solveProblem(prompt)).thenThrow(exception);
		when(openAiClient.isEnabled()).thenReturn(false);

		Throwable thrown = catchThrowable(() -> fallbackClient.solveProblem(prompt));

		assertThat(thrown).isSameAs(exception);
		verify(openAiClient, never()).solveProblem(any());
	}

	@Test
	@DisplayName("Gemini 풀이 4xx(429 제외)면 OpenAI를 호출하지 않는다")
	void solveProblem_whenGemini4xxWithout429_thenNoFallback() {
		ProblemAiSolvePrompt prompt = mock(ProblemAiSolvePrompt.class);
		GeminiAiException exception = GeminiAiException.externalCallFailed(
			HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad request", null, new byte[0], null));
		when(geminiProblemSolveAiClient.solveProblem(prompt)).thenThrow(exception);

		Throwable thrown = catchThrowable(() -> fallbackClient.solveProblem(prompt));

		assertThat(thrown).isSameAs(exception);
		verifyNoInteractions(openAiClient);
	}

	@Test
	@DisplayName("Gemini 풀이 파싱 실패 + OpenAI 활성화면 OpenAI로 fallback 한다")
	void solveProblem_whenGeminiParseFailureAndOpenAiEnabled_thenFallbackToOpenAi() {
		ProblemAiSolvePrompt prompt = mock(ProblemAiSolvePrompt.class);
		ProblemAiSolveResult openAiResult = new ProblemAiSolveResult("fallback-latex", "fallback-text");
		GeminiAiException parseException = GeminiAiException.responseParseFailed(
			new IllegalArgumentException("Structured solve response parse failed"));
		when(geminiProblemSolveAiClient.solveProblem(prompt)).thenThrow(parseException);
		when(openAiClient.isEnabled()).thenReturn(true);
		when(openAiClient.solveProblem(prompt)).thenReturn(openAiResult);

		ProblemAiSolveResult actual = fallbackClient.solveProblem(prompt);

		assertThat(actual).isEqualTo(openAiResult);
		verify(openAiClient).solveProblem(prompt);
	}

	private GeminiAiException rateLimitException() {
		return GeminiAiException.externalCallFailed(
			HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "too many", null, new byte[0], null));
	}

	private GeminiAiException serverErrorException() {
		return GeminiAiException.externalCallFailed(
			HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "unavailable", null, new byte[0], null));
	}
}
