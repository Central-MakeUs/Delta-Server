package cmc.delta.domain.problem.application.support.query;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.MyProblemListRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemListCondition;
import org.springframework.stereotype.Component;

@Component
public class ProblemListConditionFactory {

	public ProblemListCondition from(MyProblemListRequest query) {
		ProblemListSort sort = (query.sort() == null) ? ProblemListSort.RECENT : query.sort();
		ProblemStatusFilter status = (query.status() == null) ? ProblemStatusFilter.ALL : query.status();

		return new ProblemListCondition(
			trimToNull(query.subjectId()),
			trimToNull(query.unitId()),
			trimToNull(query.typeId()),
			sort,
			status
		);
	}

	private String trimToNull(String v) {
		if (v == null) return null;
		String t = v.trim();
		return t.isEmpty() ? null : t;
	}
}
