package cmc.delta.domain.problem.api.problem.dto.response;

import java.util.List;

public record ProblemStatsResponse<T>(
	List<T> items
) {
}
