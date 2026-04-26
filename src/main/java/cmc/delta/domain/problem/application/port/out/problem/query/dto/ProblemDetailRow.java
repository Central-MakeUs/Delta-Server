package cmc.delta.domain.problem.application.port.out.problem.query.dto;

import cmc.delta.domain.problem.model.enums.AnswerFormat;
import java.time.LocalDateTime;
import java.util.List;

public record ProblemDetailRow(
	Long problemId,

	String subjectId,
	String subjectName,

	String unitId,
	String unitName,

	String storageKey,

	AnswerFormat answerFormat,
	Integer answerChoiceNo,
	String answerValue,
	String memoText,

	LocalDateTime completedAt,
	LocalDateTime createdAt,

	List<ProblemTypeTagRow> types) {
}
