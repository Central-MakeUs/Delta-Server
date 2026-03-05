package cmc.delta.domain.problem.adapter.out.ai.fallback;

import cmc.delta.domain.problem.adapter.out.ai.gemini.GeminiAiClient;
import cmc.delta.domain.problem.adapter.out.ai.gemini.GeminiAiException;
import cmc.delta.domain.problem.adapter.out.ai.openai.OpenAiClient;
import cmc.delta.domain.problem.application.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class Gemini429FallbackAiClient implements AiClient {

	private final GeminiAiClient geminiAiClient;
	private final OpenAiClient openAiClient;

	@Override
	public AiCurriculumResult classifyCurriculum(AiCurriculumPrompt prompt) {
		try {
			return geminiAiClient.classifyCurriculum(prompt);
		} catch (GeminiAiException geminiAiException) {
			if (!geminiAiException.isFallbackEligibleStatus() || !openAiClient.isEnabled()) {
				throw geminiAiException;
			}
			log.warn("Gemini 분류 외부 실패(status={}) 감지, OpenAI fallback 수행", geminiAiException.httpStatus());
			return openAiClient.classifyCurriculum(prompt);
		}
	}
}
