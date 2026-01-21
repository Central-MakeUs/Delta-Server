package cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats.dto;

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
