package cmc.delta.domain.problem.adapter.in.web.problem.dto.request;

import cmc.delta.domain.problem.application.port.in.problem.command.UpdateWrongAnswerCardCommand;
import cmc.delta.domain.problem.model.enums.AnswerFormat;

public record ProblemUpdateRequest(
	Integer answerChoiceNo,
	String answerValue,
	AnswerFormat answerFormat,
	String memoText) {
	public UpdateWrongAnswerCardCommand toCommand() {
		return new UpdateWrongAnswerCardCommand(answerChoiceNo, answerValue, answerFormat, memoText);
	}
}
