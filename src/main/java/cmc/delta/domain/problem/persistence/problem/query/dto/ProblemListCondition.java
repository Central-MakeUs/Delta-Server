package cmc.delta.domain.problem.persistence.problem.query.dto;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemListSort;

public record ProblemListCondition(
	String subjectId,
	String unitId,
	String typeId,
	ProblemListSort sort
) { }
