package cmc.delta.domain.problem.adapter.in.web.problem.dto.request;

import cmc.delta.domain.problem.application.port.in.problem.command.UpdateWrongAnswerCardCommand;

public record ProblemUpdateRequest(
	Integer answerChoiceNo,
	String answerValue,
	String solutionText
) {
	public UpdateWrongAnswerCardCommand toCommand() {
		return new UpdateWrongAnswerCardCommand(answerChoiceNo, answerValue, solutionText);
	}
}
