package cmc.delta.domain.problem.application.port.in.problem;

import cmc.delta.domain.problem.application.port.in.problem.result.ProblemAiSolutionRequestResponse;

public interface ProblemAiSolutionCommandUseCase {

	ProblemAiSolutionRequestResponse requestMyProblemAiSolution(Long userId, Long problemId);
}
