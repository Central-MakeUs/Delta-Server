package cmc.delta.domain.problem.application.port.out.problem.query.dto;

import cmc.delta.domain.problem.model.enums.AnswerFormat;
import java.time.LocalDateTime;

public record ProblemDetailRow(
	Long problemId,

	String subjectId,
	String subjectName,

	String unitId,
	String unitName,

	String typeId,
	String typeName,
	Long assetId,
	String storageKey,

	AnswerFormat answerFormat,
	Integer answerChoiceNo,
	String answerValue,
	String memoText,

	LocalDateTime completedAt,
	LocalDateTime createdAt) {
}
