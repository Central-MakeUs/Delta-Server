package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemDetailResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemListCondition;
import cmc.delta.domain.problem.application.port.in.problem.ProblemQueryUseCase;
import cmc.delta.global.api.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface ProblemQueryService extends ProblemQueryUseCase {

	PagedResponse<ProblemListItemResponse> getMyProblemCardList(Long userId, ProblemListCondition condition, Pageable pageable);
	ProblemDetailResponse getMyProblemDetail(Long userId, Long problemId);
}
