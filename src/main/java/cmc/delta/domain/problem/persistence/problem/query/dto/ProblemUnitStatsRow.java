package cmc.delta.domain.problem.persistence.problem.query.dto;

public record ProblemUnitStatsRow(
	String subjectId,
	String subjectName,
	String unitId,
	String unitName,
	long solvedCount,
	long unsolvedCount,
	long totalCount
) {
}
