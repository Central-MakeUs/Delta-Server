package cmc.delta.domain.problem.api.problem.dto.request;

import cmc.delta.domain.problem.model.enums.AnswerFormat;

public record ProblemCreateRequest(
	Long scanId,
	String subjectId,
	String unitId,
	String typeId,

	AnswerFormat answerFormat,
	Integer answerChoiceNo,
	String answerValue,

	String solutionText
) { }
