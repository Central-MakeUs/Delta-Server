package cmc.delta.domain.problem.application.query;

import cmc.delta.domain.problem.api.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListCondition;
import cmc.delta.global.api.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface ProblemQueryService {

	PagedResponse<ProblemListItemResponse> getMyProblemCardList(
		Long userId,
		ProblemListCondition condition,
		Pageable pageable
	);
}
