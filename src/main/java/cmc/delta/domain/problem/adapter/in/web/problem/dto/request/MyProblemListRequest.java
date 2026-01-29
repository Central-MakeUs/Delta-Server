package cmc.delta.domain.problem.adapter.in.web.problem.dto.request;

import cmc.delta.domain.problem.model.enums.ProblemListSort;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;

public record MyProblemListRequest(
	String subjectId,
	String unitId,
	String typeId,
	ProblemListSort sort,
	ProblemStatusFilter status,
	Integer page,
	Integer size) {
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	public MyProblemListRequest {
		// sort/status 기본값
		if (sort == null)
			sort = ProblemListSort.RECENT;
		if (status == null)
			status = ProblemStatusFilter.ALL;

		if (page == null || page < 0)
			page = DEFAULT_PAGE;
		if (size == null || size <= 0)
			size = DEFAULT_SIZE;
		if (size > MAX_SIZE)
			size = MAX_SIZE; // 과도한 요청 방어
	}
}
