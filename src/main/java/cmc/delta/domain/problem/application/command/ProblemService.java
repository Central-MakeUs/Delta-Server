package cmc.delta.domain.problem.application.command;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemCreateRequest;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemCreateResponse;

public interface ProblemService {
	ProblemCreateResponse createWrongAnswerCard(Long currentUserId, ProblemCreateRequest request);
	void completeWrongAnswerCard(Long currentUserId, Long problemId, String solutionText);
}
