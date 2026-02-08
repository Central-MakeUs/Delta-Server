package cmc.delta.domain.problem.application.port.in.problem.query;

import cmc.delta.domain.problem.model.enums.ProblemListSort;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import java.util.List;

public record ProblemListCondition(
	List<String> subjectIds,
	List<String> unitIds,
	List<String> typeIds,
	ProblemListSort sort,
	ProblemStatusFilter status) {
}
