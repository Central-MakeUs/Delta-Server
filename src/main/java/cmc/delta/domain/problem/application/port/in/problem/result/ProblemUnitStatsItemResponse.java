package cmc.delta.domain.problem.application.port.in.problem.result;

import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;

public record ProblemUnitStatsItemResponse(
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	long solvedCount,
	long unsolvedCount,
	long totalCount
) {
}
