package cmc.delta.domain.problem.application.query;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemListItemResponse;
import cmc.delta.global.api.response.PagedResponse;

public interface ProblemQueryService {

	PagedResponse<ProblemListItemResponse> getMyProblemCardList(
		Long userId,
		String subjectId,
		String unitId,
		String typeId,
		ProblemListSort sort,
		int page,
		int size
	);
}
