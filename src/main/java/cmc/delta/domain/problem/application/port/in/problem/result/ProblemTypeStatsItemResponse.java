package cmc.delta.domain.problem.application.port.in.problem.result;

import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;

public record ProblemTypeStatsItemResponse(
	CurriculumItemResponse type,
	long solvedCount,
	long unsolvedCount,
	long totalCount) {
}
