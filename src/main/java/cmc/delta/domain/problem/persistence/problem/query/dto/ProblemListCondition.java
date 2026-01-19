package cmc.delta.domain.problem.persistence.problem.query.dto;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;

public record ProblemListCondition(
	String subjectId,
	String unitId,
	String typeId,
	ProblemListSort sort,
	ProblemStatusFilter status
) { }
