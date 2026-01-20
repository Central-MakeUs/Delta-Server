package cmc.delta.domain.problem.application.port.in.problem;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemCreateRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemUpdateRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemCreateResponse;

public interface ProblemCommandUseCase {

	ProblemCreateResponse createWrongAnswerCard(Long currentUserId, ProblemCreateRequest request);

	void completeWrongAnswerCard(Long currentUserId, Long problemId, String solutionText);

	void updateWrongAnswerCard(Long currentUserId, Long problemId, ProblemUpdateRequest request);
}
