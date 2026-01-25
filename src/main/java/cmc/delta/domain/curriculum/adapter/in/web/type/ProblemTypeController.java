package cmc.delta.domain.curriculum.adapter.in.web.type;

import cmc.delta.domain.curriculum.adapter.in.web.type.dto.request.ProblemTypeActivationRequest;
import cmc.delta.domain.curriculum.adapter.in.web.type.dto.request.ProblemTypeCreateRequest;
import cmc.delta.domain.curriculum.adapter.in.web.type.dto.request.ProblemTypeUpdateRequest;
import cmc.delta.domain.curriculum.application.port.in.type.ProblemTypeUseCase;
import cmc.delta.domain.curriculum.application.port.in.type.result.ProblemTypeItemResponse;
import cmc.delta.domain.curriculum.application.port.in.type.result.ProblemTypeListResponse;
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

@Tag(name = "유형")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/problem-types")
public class ProblemTypeController {

	private final ProblemTypeUseCase problemTypeUseCase;

	@Operation(summary = "내 유형 목록 조회 (기본 + 커스텀)", description = ProblemApiDocs.LIST_MY_PROBLEM_TYPES)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@GetMapping
	public ApiResponse<ProblemTypeListResponse> getMyTypes(
		@CurrentUser
		UserPrincipal principal,
		@RequestParam(name = "includeInactive", required = false, defaultValue = "false")
		boolean includeInactive) {
		ProblemTypeListResponse data = problemTypeUseCase.getMyTypes(principal.userId(), includeInactive);
		return ApiResponses.success(SuccessCode.OK, data);
	}

	@Operation(summary = "커스텀 유형 추가", description = ProblemApiDocs.CREATE_CUSTOM_PROBLEM_TYPE)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.INVALID_REQUEST,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@PostMapping
	public ApiResponse<ProblemTypeItemResponse> createCustomType(
		@CurrentUser
		UserPrincipal principal,
		@RequestBody
		ProblemTypeCreateRequest request) {
		ProblemTypeItemResponse data = problemTypeUseCase.createCustomType(principal.userId(), request.toCommand());
		return ApiResponses.success(SuccessCode.OK, data);
	}

	@Operation(summary = "커스텀 유형 수정 (이름/순서)", description = ProblemApiDocs.UPDATE_CUSTOM_PROBLEM_TYPE)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.INVALID_REQUEST,
		ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@PatchMapping("/{typeId}")
	public ApiResponse<ProblemTypeItemResponse> updateCustomType(
		@CurrentUser
		UserPrincipal principal,
		@PathVariable
		String typeId,
		@RequestBody
		ProblemTypeUpdateRequest request) {
		ProblemTypeItemResponse data = problemTypeUseCase.updateCustomType(principal.userId(), typeId,
			request.toCommand());
		return ApiResponses.success(SuccessCode.OK, data);
	}

	@Operation(summary = "커스텀 유형 활성/비활성", description = ProblemApiDocs.SET_CUSTOM_PROBLEM_TYPE_ACTIVE)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.INVALID_REQUEST,
		ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@PatchMapping("/{typeId}/activation")
	public ApiResponse<Void> setCustomTypeActive(
		@CurrentUser
		UserPrincipal principal,
		@PathVariable
		String typeId,
		@RequestBody
		ProblemTypeActivationRequest request) {
		problemTypeUseCase.setActive(principal.userId(), typeId, request.toCommand());
		return ApiResponses.success(SuccessCode.OK, null);
	}
}
