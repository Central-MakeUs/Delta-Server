package cmc.delta.domain.problem.adapter.in.web.problem.dto.request;

import cmc.delta.domain.problem.model.enums.ProblemStatsSort;

public record ProblemStatsRequest(
	String subjectId,
	String unitId,
	String typeId,
	ProblemStatsSort sort
) {
	public ProblemStatsRequest {
		if (sort == null) sort = ProblemStatsSort.DEFAULT;
	}
}