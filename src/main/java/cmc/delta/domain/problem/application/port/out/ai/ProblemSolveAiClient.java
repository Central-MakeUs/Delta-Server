package cmc.delta.domain.problem.application.port.out.ai;

import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolvePrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolveResult;

public interface ProblemSolveAiClient {

	ProblemAiSolveResult solveProblem(ProblemAiSolvePrompt prompt);
}
