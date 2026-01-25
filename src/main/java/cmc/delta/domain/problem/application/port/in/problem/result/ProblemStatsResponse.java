package cmc.delta.domain.problem.application.port.in.problem.result;

import java.util.List;

public record ProblemStatsResponse<T>(
	List<T> items) {
}
