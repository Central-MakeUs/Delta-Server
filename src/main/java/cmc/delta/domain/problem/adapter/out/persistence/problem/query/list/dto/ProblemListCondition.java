package cmc.delta.domain.problem.adapter.out.persistence.problem.query.list.dto;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;

public record ProblemListCondition(
	String subjectId,
	String unitId,
	String typeId,
	ProblemListSort sort,
	ProblemStatusFilter status
) { }
