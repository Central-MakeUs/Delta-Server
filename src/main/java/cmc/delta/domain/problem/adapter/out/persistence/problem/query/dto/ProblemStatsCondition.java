package cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto;

import cmc.delta.domain.problem.model.enums.ProblemStatsSort;

public record ProblemStatsCondition(
	String subjectId,
	String unitId,
	String typeId,
	ProblemStatsSort sort
) {
}
