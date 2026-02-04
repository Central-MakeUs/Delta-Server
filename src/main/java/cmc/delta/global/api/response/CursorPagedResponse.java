package cmc.delta.global.api.response;

import java.time.LocalDateTime;
import java.util.List;

public record CursorPagedResponse<T>(
	List<T> content,
	boolean hasNext,
	Cursor nextCursor,
	Long totalElements) {

	public static <T> CursorPagedResponse<T> of(
		List<T> content,
		boolean hasNext,
		Cursor nextCursor,
		Long totalElements) {
		return new CursorPagedResponse<>(content, hasNext, nextCursor, totalElements);
	}

	public record Cursor(
		Long lastId,
		LocalDateTime lastCreatedAt) {
	}
}
