package cmc.delta.domain.problem.application.port.in.problem.command;

import java.util.List;

import cmc.delta.domain.problem.model.enums.AnswerFormat;

public record CreateWrongAnswerCardCommand(
	Long scanId,
	String finalUnitId,
	List<String> finalTypeIds,
	AnswerFormat answerFormat,
	Integer answerChoiceNo,
	String answerValue,
	String solutionText
) {}
