package cmc.delta.domain.problem.api.problem.dto.request;

import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;

public record MyProblemListRequest(
	String subjectId,
	String unitId,
	String typeId,
	ProblemListSort sort,
	ProblemStatusFilter status,
	Integer page,
	Integer size
) {
	public MyProblemListRequest {
		// sort/status 기본값
		if (sort == null) sort = ProblemListSort.RECENT;
		if (status == null) status = ProblemStatusFilter.ALL;

		if (page == null || page < 0) page = 0;
		if (size == null || size <= 0) size = 20;
		if (size > 100) size = 100; // 과도한 요청 방어
	}
}
