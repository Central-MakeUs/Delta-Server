package cmc.delta.domain.problem.api.problem.dto.request;

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