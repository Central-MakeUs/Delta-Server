package cmc.delta.domain.problem.api.problem.dto.request;

import cmc.delta.domain.problem.model.enums.AnswerFormat;

public record ProblemCreateRequest(
	Long scanId,
	String finalUnitId,
	String finalTypeId,

	AnswerFormat answerFormat,
	Integer answerChoiceNo,
	String answerValue,

	String solutionText
) { }
