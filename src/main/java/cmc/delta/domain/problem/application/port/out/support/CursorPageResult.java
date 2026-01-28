package cmc.delta.domain.problem.application.port.out.support;

import java.time.LocalDateTime;
import java.util.List;

public record CursorPageResult<T>(
	List<T> content,
	boolean hasNext,
	Long nextLastId,
	LocalDateTime nextLastCreatedAt,
	Long totalElements) {
}
