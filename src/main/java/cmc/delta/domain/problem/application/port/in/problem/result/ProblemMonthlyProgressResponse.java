package cmc.delta.domain.problem.application.port.in.problem.result;

public record ProblemMonthlyProgressResponse(
	String yearMonth,
	long totalCount,
	long solvedCount,
	long unsolvedCount) {
}
