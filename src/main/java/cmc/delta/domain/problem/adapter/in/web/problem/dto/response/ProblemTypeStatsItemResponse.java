package cmc.delta.domain.problem.adapter.in.web.problem.dto.response;

public record ProblemTypeStatsItemResponse(
	CurriculumItemResponse type,
	long solvedCount,
	long unsolvedCount,
	long totalCount
) {
}
