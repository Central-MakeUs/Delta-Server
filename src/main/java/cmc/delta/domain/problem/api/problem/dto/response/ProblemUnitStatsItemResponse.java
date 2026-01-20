package cmc.delta.domain.problem.api.problem.dto.response;

public record ProblemUnitStatsItemResponse(
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	long solvedCount,
	long unsolvedCount,
	long totalCount
) {
}
