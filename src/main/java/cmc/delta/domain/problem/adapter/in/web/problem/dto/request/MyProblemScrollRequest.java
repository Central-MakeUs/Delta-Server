package cmc.delta.domain.problem.adapter.in.web.problem.dto.request;

import cmc.delta.domain.problem.model.enums.ProblemListSort;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 무한 스크롤(커서 기반) 목록 조회.
 * - 첫 요청: lastId/lastCreatedAt 없이 호출
 * - 다음 요청: 직전 응답의 nextCursor 값을 그대로 전달
 */
public record MyProblemScrollRequest(
	List<String> subjectIds,
	List<String> unitIds,
	List<String> typeIds,
	ProblemListSort sort,
	ProblemStatusFilter status,
	Long lastId,
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	LocalDateTime lastCreatedAt,
	Integer size,
	Boolean includePreviewUrl) {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 50;

	public MyProblemScrollRequest {
		// sort/status 기본값
		if (sort == null)
			sort = ProblemListSort.RECENT;
		if (status == null)
			status = ProblemStatusFilter.ALL;

		if (size == null || size <= 0)
			size = DEFAULT_SIZE;
		if (size > MAX_SIZE)
			size = MAX_SIZE; // validator MAX_SIZE와 동일

		// default true for backward compatibility
		if (includePreviewUrl == null)
			includePreviewUrl = true;
	}
}
