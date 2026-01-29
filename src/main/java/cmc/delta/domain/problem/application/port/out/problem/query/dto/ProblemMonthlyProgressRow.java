package cmc.delta.domain.problem.application.port.out.problem.query.dto;

public record ProblemMonthlyProgressRow(
	long totalCount,
	long solvedCount,
	long unsolvedCount) {
}
