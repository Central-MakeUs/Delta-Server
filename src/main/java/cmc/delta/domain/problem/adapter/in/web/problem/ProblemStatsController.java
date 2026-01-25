package cmc.delta.domain.problem.adapter.in.web.problem;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemStatsRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.support.ProblemStatsConditionFactory;
import cmc.delta.domain.problem.application.port.in.problem.ProblemStatsUseCase;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemStatsCondition;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemStatsResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemTypeStatsItemResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemUnitStatsItemResponse;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.security.principal.CurrentUser;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
import cmc.delta.global.config.swagger.ProblemApiDocs;
import cmc.delta.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "오답 통계")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/problems/stats")
public class ProblemStatsController {

	private final ProblemStatsUseCase statsUseCase;
	private final ProblemStatsConditionFactory statsConditionFactory;

	@Operation(summary = "단원별 오답 통계(완료/미완료)", description = ProblemApiDocs.STATS_BY_UNIT)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@GetMapping("/units")
	public ApiResponse<ProblemStatsResponse<ProblemUnitStatsItemResponse>> getMyUnitStats(
		@CurrentUser
		UserPrincipal principal,
		@ModelAttribute
		ProblemStatsRequest query) {
		ProblemStatsCondition condition = statsConditionFactory.from(query);
		ProblemStatsResponse<ProblemUnitStatsItemResponse> data = statsUseCase.getUnitStats(principal.userId(),
			condition);
		return ApiResponses.success(SuccessCode.OK, data);
	}

	@Operation(summary = "유형별 오답 통계(완료/미완료)", description = ProblemApiDocs.STATS_BY_TYPE)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@GetMapping("/types")
	public ApiResponse<ProblemStatsResponse<ProblemTypeStatsItemResponse>> getMyTypeStats(
		@CurrentUser
		UserPrincipal principal,
		@ModelAttribute
		ProblemStatsRequest query) {
		ProblemStatsCondition condition = statsConditionFactory.from(query);
		ProblemStatsResponse<ProblemTypeStatsItemResponse> data = statsUseCase.getTypeStats(principal.userId(),
			condition);
		return ApiResponses.success(SuccessCode.OK, data);
	}
}
