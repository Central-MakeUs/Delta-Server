package cmc.delta.domain.problem.application.port.out.support;

import java.util.List;

public record PageResult<T>(
	List<T> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {
}
