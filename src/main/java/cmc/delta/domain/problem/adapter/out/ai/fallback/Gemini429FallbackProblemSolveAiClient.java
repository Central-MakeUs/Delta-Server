package cmc.delta.domain.problem.adapter.out.ai.fallback;

import cmc.delta.domain.problem.adapter.out.ai.gemini.GeminiAiException;
import cmc.delta.domain.problem.adapter.out.ai.gemini.GeminiProblemSolveAiClient;
import cmc.delta.domain.problem.adapter.out.ai.openai.OpenAiClient;
import cmc.delta.domain.problem.application.port.out.ai.ProblemSolveAiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolvePrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolveResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class Gemini429FallbackProblemSolveAiClient implements ProblemSolveAiClient {

	private final GeminiProblemSolveAiClient geminiProblemSolveAiClient;
	private final OpenAiClient openAiClient;

	@Override
	public ProblemAiSolveResult solveProblem(ProblemAiSolvePrompt prompt) {
		try {
			return geminiProblemSolveAiClient.solveProblem(prompt);
		} catch (GeminiAiException geminiAiException) {
			if (!geminiAiException.isRateLimited() || !openAiClient.isEnabled()) {
				throw geminiAiException;
			}
			log.warn("Gemini 풀이 429 감지, OpenAI fallback 수행");
			return openAiClient.solveProblem(prompt);
		}
	}
}
