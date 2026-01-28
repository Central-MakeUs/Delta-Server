package cmc.delta.domain.problem.application.port.in.problem;

import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemDetailResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemListItemResponse;
import cmc.delta.domain.problem.application.port.in.support.CursorQuery;
import cmc.delta.domain.problem.application.port.in.support.PageQuery;
import cmc.delta.global.api.response.CursorPagedResponse;
import cmc.delta.global.api.response.PagedResponse;

public interface ProblemQueryUseCase {

	PagedResponse<ProblemListItemResponse> getMyProblemCardList(
		Long userId,
		ProblemListCondition condition,
		PageQuery pageQuery);

	CursorPagedResponse<ProblemListItemResponse> getMyProblemCardListCursor(
		Long userId,
		ProblemListCondition condition,
		CursorQuery cursorQuery);

	CursorPagedResponse<ProblemListItemResponse> getMyProblemCardListCursor(
		Long userId,
		ProblemListCondition condition,
		CursorQuery cursorQuery,
		boolean includePreviewUrl);

	ProblemDetailResponse getMyProblemDetail(Long userId, Long problemId);
}
