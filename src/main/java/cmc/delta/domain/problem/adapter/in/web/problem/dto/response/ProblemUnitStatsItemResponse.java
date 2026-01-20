package cmc.delta.domain.problem.adapter.in.web.problem.dto.response;

public record ProblemUnitStatsItemResponse(
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	long solvedCount,
	long unsolvedCount,
	long totalCount
) {
}
