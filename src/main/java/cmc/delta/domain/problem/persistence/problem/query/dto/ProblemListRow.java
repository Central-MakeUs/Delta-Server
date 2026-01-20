package cmc.delta.domain.problem.persistence.problem.query.dto;

import java.time.LocalDateTime;

public record ProblemListRow(
	Long problemId,

	String subjectId,
	String subjectName,

	String unitId,
	String unitName,

	String typeId,
	String typeName,

	Long assetId,
	String storageKey,

	LocalDateTime createdAt
) {
}
