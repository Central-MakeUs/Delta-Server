package cmc.delta.domain.problem.application.port.in.problem.query;

import cmc.delta.domain.problem.model.enums.ProblemStatsSort;

public record ProblemStatsCondition(
	String subjectId,
	String unitId,
	String typeId,
	ProblemStatsSort sort) {
}
