package cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail;

import java.time.LocalDateTime;
import java.util.List;

import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeTagRow;
import cmc.delta.domain.problem.model.enums.AnswerFormat;

public record ProblemDetailFlatRow(
	Long problemId,
	String subjectId, String subjectName,
	String unitId, String unitName,
	String storageKey,
	AnswerFormat answerFormat,
	Integer answerChoiceNo,
	String answerValue,
	String memoText,
	LocalDateTime completedAt,
	LocalDateTime createdAt,
	String typeId, String typeName) {

	ProblemDetailRow toProblemDetailRow(List<ProblemTypeTagRow> types) {
		return new ProblemDetailRow(
			problemId,
			subjectId, subjectName,
			unitId, unitName,
			storageKey,
			answerFormat,
			answerChoiceNo,
			answerValue,
			memoText,
			completedAt,
			createdAt,
			types);
	}
}
