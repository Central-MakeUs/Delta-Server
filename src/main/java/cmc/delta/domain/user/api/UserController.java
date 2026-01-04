package cmc.delta.domain.user.api;

import cmc.delta.domain.user.api.dto.response.UserMeData;
import cmc.delta.domain.user.application.service.UserService;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.config.security.principal.CurrentUser;
import cmc.delta.global.config.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public UserMeData getMyProfile(@CurrentUser UserPrincipal principal) {
		return userService.getMyProfile(principal.userId());
	}

	@PostMapping("/withdrawal")
	public ApiResponse<Void> withdrawMyAccount(@CurrentUser UserPrincipal principal) {
		userService.withdrawAccount(principal.userId());
		return ApiResponses.success(200);
	}
}
