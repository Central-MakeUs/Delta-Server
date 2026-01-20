package cmc.delta.domain.problem.persistence.problem.query.dto;

public record ProblemTypeStatsRow(
	String typeId,
	String typeName,
	long solvedCount,
	long unsolvedCount,
	long totalCount
) {
}
