package cmc.delta.domain.problem.application.port.in.support;

import java.time.LocalDateTime;

public record CursorQuery(
	Long lastId,
	LocalDateTime lastCreatedAt,
	int size) {
	public boolean isFirstPage() {
		return lastId == null && lastCreatedAt == null;
	}
}
