package cmc.delta.domain.problem.application.port.in.problem.command;

import cmc.delta.domain.problem.model.enums.AnswerFormat;

public record CreateWrongAnswerCardCommand(
	Long scanId,
	String finalUnitId,
	String finalTypeId,
	AnswerFormat answerFormat,
	Integer answerChoiceNo,
	String answerValue,
	String solutionText
) {}
