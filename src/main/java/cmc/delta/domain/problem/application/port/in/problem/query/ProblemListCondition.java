package cmc.delta.domain.problem.application.port.in.problem.query;

import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import cmc.delta.domain.problem.model.enums.ProblemListSort;

public record ProblemListCondition(
	String subjectId,
	String unitId,
	String typeId,
	ProblemListSort sort,
	ProblemStatusFilter status
) { }
