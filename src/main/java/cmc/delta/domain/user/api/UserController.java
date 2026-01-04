package cmc.delta.domain.user.api;

import cmc.delta.domain.user.api.dto.response.UserMeData;
import cmc.delta.domain.user.application.service.UserService;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
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

	private final UserService userService;

	@Operation(summary = "내 프로필 조회")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN
	})
	@GetMapping("/me")
	public UserMeData getMyProfile(@CurrentUser UserPrincipal principal) {
		return userService.getMyProfile(principal.userId());
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
		userService.withdrawAccount(principal.userId());
		return ApiResponses.success(200);
	}
}
