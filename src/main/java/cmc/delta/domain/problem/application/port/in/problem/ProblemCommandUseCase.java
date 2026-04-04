package cmc.delta.domain.problem.application.port.in.problem;

import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.port.in.problem.command.UpdateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemBulkCreateResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemCreateResponse;
import java.util.List;

public interface ProblemCommandUseCase {

	ProblemCreateResponse createWrongAnswerCard(Long currentUserId, CreateWrongAnswerCardCommand request);

	ProblemBulkCreateResponse createBulkWrongAnswerCards(Long currentUserId,
		List<CreateWrongAnswerCardCommand> commands);

	void completeWrongAnswerCard(Long currentUserId, Long problemId, String memoText);

	void updateWrongAnswerCard(Long currentUserId, Long problemId, UpdateWrongAnswerCardCommand request);

	void deleteWrongAnswerCard(Long currentUserId, Long problemId);
}
