package cmc.delta.domain.problem.application.support.query;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemStatsRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemStatsSort;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemStatsCondition;
import org.springframework.stereotype.Component;

@Component
public class ProblemStatsConditionFactory {

	public ProblemStatsCondition from(ProblemStatsRequest query) {
		ProblemStatsSort sort = (query.sort() == null) ? ProblemStatsSort.DEFAULT : query.sort();

		return new ProblemStatsCondition(
			trimToNull(query.subjectId()),
			trimToNull(query.unitId()),
			trimToNull(query.typeId()),
			sort
		);
	}

	private String trimToNull(String v) {
		if (v == null) return null;
		String t = v.trim();
		return t.isEmpty() ? null : t;
	}
}