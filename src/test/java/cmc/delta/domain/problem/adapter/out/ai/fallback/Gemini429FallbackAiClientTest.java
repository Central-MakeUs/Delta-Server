package cmc.delta.domain.problem.adapter.out.ai.fallback;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.out.ai.gemini.GeminiAiClient;
import cmc.delta.domain.problem.adapter.out.ai.gemini.GeminiAiException;
import cmc.delta.domain.problem.adapter.out.ai.openai.OpenAiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class Gemini429FallbackAiClientTest {

	private final GeminiAiClient geminiAiClient = mock(GeminiAiClient.class);
	private final OpenAiClient openAiClient = mock(OpenAiClient.class);
	private final Gemini429FallbackAiClient fallbackClient = new Gemini429FallbackAiClient(geminiAiClient,
		openAiClient);

	@Test
	@DisplayName("Gemini 성공이면 OpenAI fallback 없이 Gemini 결과를 사용한다")
	void classifyCurriculum_whenGeminiSucceeded_thenReturnsGeminiResult() {
		AiCurriculumPrompt prompt = mock(AiCurriculumPrompt.class);
		AiCurriculumResult geminiResult = mock(AiCurriculumResult.class);
		when(geminiAiClient.classifyCurriculum(prompt)).thenReturn(geminiResult);

		AiCurriculumResult actual = fallbackClient.classifyCurriculum(prompt);

		assertThat(actual).isSameAs(geminiResult);
		verifyNoInteractions(openAiClient);
	}

	@Test
	@DisplayName("Gemini 429 + OpenAI 활성화면 OpenAI로 1회 fallback 한다")
	void classifyCurriculum_whenGemini429AndOpenAiEnabled_thenFallbackToOpenAi() {
		AiCurriculumPrompt prompt = mock(AiCurriculumPrompt.class);
		AiCurriculumResult openAiResult = mock(AiCurriculumResult.class);
		when(geminiAiClient.classifyCurriculum(prompt)).thenThrow(rateLimitException());
		when(openAiClient.isEnabled()).thenReturn(true);
		when(openAiClient.classifyCurriculum(prompt)).thenReturn(openAiResult);

		AiCurriculumResult actual = fallbackClient.classifyCurriculum(prompt);

		assertThat(actual).isSameAs(openAiResult);
		verify(openAiClient).classifyCurriculum(prompt);
	}

	@Test
	@DisplayName("Gemini 429 + OpenAI 비활성화면 Gemini 예외를 그대로 던진다")
	void classifyCurriculum_whenGemini429AndOpenAiDisabled_thenThrowsGeminiException() {
		AiCurriculumPrompt prompt = mock(AiCurriculumPrompt.class);
		GeminiAiException exception = rateLimitException();
		when(geminiAiClient.classifyCurriculum(prompt)).thenThrow(exception);
		when(openAiClient.isEnabled()).thenReturn(false);

		Throwable thrown = catchThrowable(() -> fallbackClient.classifyCurriculum(prompt));

		assertThat(thrown).isSameAs(exception);
		verify(openAiClient, never()).classifyCurriculum(any());
	}

	@Test
	@DisplayName("Gemini 429가 아니면 OpenAI를 호출하지 않는다")
	void classifyCurriculum_whenGeminiNot429_thenNoFallback() {
		AiCurriculumPrompt prompt = mock(AiCurriculumPrompt.class);
		GeminiAiException exception = GeminiAiException.externalCallFailed(
			HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad request", null, new byte[0], null));
		when(geminiAiClient.classifyCurriculum(prompt)).thenThrow(exception);

		Throwable thrown = catchThrowable(() -> fallbackClient.classifyCurriculum(prompt));

		assertThat(thrown).isSameAs(exception);
		verifyNoInteractions(openAiClient);
	}

	private GeminiAiException rateLimitException() {
		return GeminiAiException.externalCallFailed(
			HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "too many", null, new byte[0], null));
	}
}
