package cmc.delta.domain.problem.persistence.problem.query.dto;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemStatsSort;

public record ProblemStatsCondition(
	String subjectId,
	String unitId,
	String typeId,
	ProblemStatsSort sort
) {
}
