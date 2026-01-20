package cmc.delta.domain.problem.api.problem.dto.response;

public record ProblemTypeStatsItemResponse(
	CurriculumItemResponse type,
	long solvedCount,
	long unsolvedCount,
	long totalCount
) {
}
