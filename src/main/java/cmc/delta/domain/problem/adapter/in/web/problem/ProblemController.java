package cmc.delta.domain.problem.adapter.in.web.problem;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.MyProblemListRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemCompleteRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemCreateRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemUpdateRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemCreateResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemDetailResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.application.port.in.problem.ProblemCommandUseCase;
import cmc.delta.domain.problem.application.port.in.problem.ProblemQueryUseCase;
import cmc.delta.domain.problem.application.support.query.ProblemListConditionFactory;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.list.dto.ProblemListCondition;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.security.principal.CurrentUser;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
import cmc.delta.global.config.swagger.ProblemApiDocs;
import cmc.delta.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Tag(name = "오답카드")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/problems")
public class ProblemController {

	private final ProblemCommandUseCase problemCommandUseCase;
	private final ProblemQueryUseCase problemQueryUseCase;
	private final ProblemListConditionFactory conditionFactory;

	@Operation(
		summary = "오답카드 생성 (scan 기반 최종 저장)",
		description = ProblemApiDocs.CREATE_WRONG_ANSWER_CARD
	)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.INVALID_REQUEST,
		ErrorCode.PROBLEM_SCAN_NOT_FOUND,
		ErrorCode.PROBLEM_ASSET_NOT_FOUND,
		ErrorCode.PROBLEM_ALREADY_CREATED,
		ErrorCode.INTERNAL_ERROR
	})
	@PostMapping
	public ApiResponse<ProblemCreateResponse> createWrongAnswerCard(
		@CurrentUser UserPrincipal principal,
		@RequestBody ProblemCreateRequest request
	) {
		ProblemCreateResponse data = problemCommandUseCase.createWrongAnswerCard(principal.userId(), request.toCommand());
		return ApiResponses.success(SuccessCode.OK, data);
	}

	@Operation(
		summary = "내 오답 카드 목록 조회",
		description = ProblemApiDocs.LIST_MY_PROBLEMS
	)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.PROBLEM_LIST_INVALID_PAGINATION,
		ErrorCode.INTERNAL_ERROR
	})
	@GetMapping
	public ApiResponse<PagedResponse<ProblemListItemResponse>> getMyProblemList(
		@CurrentUser UserPrincipal principal,
		@ModelAttribute MyProblemListRequest query
	) {
		Pageable pageable = PageRequest.of(query.page(), query.size());
		ProblemListCondition condition = conditionFactory.from(query);

		PagedResponse<ProblemListItemResponse> data =
			problemQueryUseCase.getMyProblemCardList(principal.userId(), condition, pageable);

		return ApiResponses.success(SuccessCode.OK, data);
	}

	@Operation(
		summary = "오답카드 오답 완료 처리",
		description = ProblemApiDocs.COMPLETE_WRONG_ANSWER_CARD
	)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.INTERNAL_ERROR
	})
	@PostMapping("/{problemId}/complete")
	public ApiResponse<Void> completeWrongAnswerCard(
		@CurrentUser UserPrincipal principal,
		@PathVariable Long problemId,
		@RequestBody ProblemCompleteRequest request
	) {
		problemCommandUseCase.completeWrongAnswerCard(principal.userId(), problemId, request.solutionText());
		return ApiResponses.success(SuccessCode.OK, null);
	}

	@Operation(summary = "내 오답카드 상세 조회",
	description = ProblemApiDocs.GET_MY_PROBLEM_DETAIL)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@GetMapping("/{problemId}")
	public ApiResponse<ProblemDetailResponse> getMyProblemDetail(
		@CurrentUser UserPrincipal principal,
		@PathVariable Long problemId
	) {
		ProblemDetailResponse data = problemQueryUseCase.getMyProblemDetail(principal.userId(), problemId);

		return ApiResponses.success(SuccessCode.OK, data);
	}

	@Operation(
		summary = "오답카드 정답/풀이 수정",
		description = ProblemApiDocs.UPDATE_WRONG_ANSWER_CARD
	)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.INVALID_REQUEST,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@PatchMapping("/{problemId}")
	public ApiResponse<Void> updateWrongAnswerCard(
		@CurrentUser UserPrincipal principal,
		@PathVariable Long problemId,
		@RequestBody ProblemUpdateRequest request
	) {
		problemCommandUseCase.updateWrongAnswerCard(principal.userId(), problemId, request.toCommand());
		return ApiResponses.success(SuccessCode.OK, null);
	}
}
