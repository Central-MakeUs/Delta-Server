package cmc.delta.domain.problem.application.port.out.problem.query.dto;

import java.time.LocalDateTime;

public record ProblemListRow(
	Long problemId,

	String subjectId,
	String subjectName,

	String unitId,
	String unitName,

	String typeId,
	String typeName,
	String storageKey,

	LocalDateTime completedAt,

	LocalDateTime createdAt) {
}
