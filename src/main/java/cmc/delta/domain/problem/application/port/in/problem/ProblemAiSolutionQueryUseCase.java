package cmc.delta.domain.problem.application.port.in.problem;

import cmc.delta.domain.problem.application.port.in.problem.result.ProblemAiSolutionDetailResponse;

public interface ProblemAiSolutionQueryUseCase {

	ProblemAiSolutionDetailResponse getMyProblemAiSolution(Long userId, Long problemId);
}
