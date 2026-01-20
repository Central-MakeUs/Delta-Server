package cmc.delta.domain.problem.adapter.in.web.problem.dto.response;

import java.util.List;

public record ProblemStatsResponse<T>(
	List<T> items
) {
}
