package cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats.dto;

public record ProblemTypeStatsRow(
	String typeId,
	String typeName,
	long solvedCount,
	long unsolvedCount,
	long totalCount
) {
}
