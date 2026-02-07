package cmc.delta.domain.problem.application.port.in.problem;

import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.port.in.problem.command.UpdateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemCreateResponse;

public interface ProblemCommandUseCase {

	ProblemCreateResponse createWrongAnswerCard(Long currentUserId, CreateWrongAnswerCardCommand request);

	void completeWrongAnswerCard(Long currentUserId, Long problemId, String solutionText);

	void updateWrongAnswerCard(Long currentUserId, Long problemId, UpdateWrongAnswerCardCommand request);

	void deleteWrongAnswerCard(Long currentUserId, Long problemId);
}
