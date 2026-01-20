package cmc.delta.domain.problem.application.port.out.ai;

import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;

public interface AiClient {
	AiCurriculumResult classifyCurriculum(AiCurriculumPrompt prompt);
}
