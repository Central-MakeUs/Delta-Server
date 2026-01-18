package cmc.delta.domain.problem.api.problem;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemCreateRequest;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemCreateResponse;
import cmc.delta.domain.problem.application.command.ProblemService;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.security.principal.CurrentUser;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
import cmc.delta.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "오답카드")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/problems")
public class ProblemController {

	private final ProblemService problemService;

	@Operation(summary = "오답카드 생성 (scan 기반 최종 저장)")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.INVALID_REQUEST,
		ErrorCode.PROBLEM_SCAN_NOT_FOUND,
		ErrorCode.PROBLEM_ASSET_NOT_FOUND,
		// 있으면 추천: ErrorCode.PROBLEM_ALREADY_CREATED
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
}
