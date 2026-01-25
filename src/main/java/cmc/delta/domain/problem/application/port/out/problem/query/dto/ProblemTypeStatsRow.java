package cmc.delta.domain.problem.application.port.out.problem.query.dto;

public record ProblemTypeStatsRow(
	String typeId,
	String typeName,
	long solvedCount,
	long unsolvedCount,
	long totalCount
) {
}
