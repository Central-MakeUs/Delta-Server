package cmc.delta.domain.user.adapter.in;

import cmc.delta.domain.user.adapter.in.dto.response.UserMeData;
import cmc.delta.domain.user.application.port.in.UserUseCase;
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

@Tag(name = "유저")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserUseCase userUseCase;

	@Operation(summary = "내 프로필 조회")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN
	})
	@GetMapping("/me")
	public ApiResponse<UserMeData> getMyProfile(@CurrentUser UserPrincipal principal) {
		UserMeData data = userUseCase.getMyProfile(principal.userId());
		return ApiResponses.success(SuccessCode.OK, data);
	}

	@Operation(summary = "회원 탈퇴")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN
	})
	@PostMapping("/withdrawal")
	public ApiResponse<Void> withdrawMyAccount(@CurrentUser UserPrincipal principal) {
		userUseCase.withdrawAccount(principal.userId());
		return ApiResponses.success(SuccessCode.OK);
	}
}
