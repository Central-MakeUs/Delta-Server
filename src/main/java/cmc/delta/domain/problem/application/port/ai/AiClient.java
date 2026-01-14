package cmc.delta.domain.problem.application.port.ai;

public interface AiClient {
	AiCurriculumResult classifyCurriculum(AiCurriculumPrompt prompt);
}
