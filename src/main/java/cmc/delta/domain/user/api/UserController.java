package cmc.delta.domain.user.api;

import cmc.delta.domain.user.application.dto.response.UserActionResultData;
import cmc.delta.domain.user.application.dto.response.UserMeData;
import cmc.delta.domain.user.application.withdrawal.UserWithdrawalService;
import cmc.delta.domain.user.persistence.UserJpaRepository;
import cmc.delta.global.config.security.principal.CurrentUser;
import cmc.delta.global.config.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserJpaRepository userJpaRepository;
	private final UserWithdrawalService userWithdrawalService;

	@GetMapping("/me")
	public UserMeData me(@CurrentUser UserPrincipal principal) {
		return userJpaRepository.findById(principal.userId())
			.map(u -> new UserMeData(u.getId(), u.getEmail(), u.getNickname()))
			.orElseThrow(() -> cmc.delta.domain.user.application.exception.UserException.userNotFound());
	}

	@PostMapping("/withdrawal")
	public UserActionResultData withdraw(@CurrentUser UserPrincipal principal) {
		userWithdrawalService.withdraw(principal.userId());
		return UserActionResultData.success();
	}
}
