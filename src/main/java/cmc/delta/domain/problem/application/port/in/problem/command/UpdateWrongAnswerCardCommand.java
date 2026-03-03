package cmc.delta.domain.problem.application.port.in.problem.command;

import cmc.delta.domain.problem.model.enums.AnswerFormat;

public record UpdateWrongAnswerCardCommand(
	Integer answerChoiceNo,
	String answerValue,
	AnswerFormat answerFormat,
	String memoText) {
}
