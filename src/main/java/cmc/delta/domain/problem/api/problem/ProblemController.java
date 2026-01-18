package cmc.delta.domain.problem.api.problem;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemCreateRequest;
import cmc.delta.domain.problem.api.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemCreateResponse;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.application.command.ProblemService;
import cmc.delta.domain.problem.application.query.ProblemQueryService;
import cmc.delta.domain.problem.persistence.problem.dto.ProblemListCondition;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.security.principal.CurrentUser;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
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

	private final ProblemService problemService;
	private final ProblemQueryService problemQueryService;

	@Operation(summary = "오답카드 생성 (scan 기반 최종 저장)")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.INVALID_REQUEST,
		ErrorCode.PROBLEM_SCAN_NOT_FOUND,
		ErrorCode.PROBLEM_ASSET_NOT_FOUND,
		ErrorCode.INTERNAL_ERROR
	})
	@PostMapping
	public ApiResponse<ProblemCreateResponse> createWrongAnswerCard(
		@CurrentUser UserPrincipal principal,
		@RequestBody ProblemCreateRequest request
	) {
		ProblemCreateResponse data = problemService.createWrongAnswerCard(principal.userId(), request);
		return ApiResponses.success(SuccessCode.OK, data);
	}

	@Operation(summary = "내 오답 카드 목록 조회")
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
		@RequestParam(required = false) String subjectId,
		@RequestParam(required = false) String unitId,
		@RequestParam(required = false) String typeId,
		@RequestParam(defaultValue = "RECENT") ProblemListSort sort,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		Pageable pageable = PageRequest.of(page, size);
		ProblemListCondition condition = new ProblemListCondition(subjectId, unitId, typeId, sort);

		PagedResponse<ProblemListItemResponse> data =
			problemQueryService.getMyProblemCardList(principal.userId(), condition, pageable);

		return ApiResponses.success(SuccessCode.OK, data);
	}
}
