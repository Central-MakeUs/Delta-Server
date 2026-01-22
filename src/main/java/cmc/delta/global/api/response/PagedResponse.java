package cmc.delta.global.api.response;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PagedResponse<T>(
	List<T> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {
	public static <T> PagedResponse<T> from(Page<T> pageData) {
		return new PagedResponse<T>(
			pageData.getContent(),
			pageData.getNumber(),
			pageData.getSize(),
			pageData.getTotalElements(),
			pageData.getTotalPages()
		);
	}

	public static <E, R> PagedResponse<R> of(Page<E> pageData, Function<E, R> converter) {
		List<R> content = pageData.getContent().stream()
			.map(converter)
			.toList();

		return new PagedResponse<R>(
			content,
			pageData.getNumber(),
			pageData.getSize(),
			pageData.getTotalElements(),
			pageData.getTotalPages()
		);
	}

	public static <E, R> PagedResponse<R> of(Page<E> pageData, List<R> content) {
		return new PagedResponse<R>(
			content,
			pageData.getNumber(),
			pageData.getSize(),
			pageData.getTotalElements(),
			pageData.getTotalPages()
		);
	}
}
