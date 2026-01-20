package cmc.delta.domain.auth.application.service.social;

import cmc.delta.domain.auth.adapter.in.web.dto.response.SocialLoginData;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.auth.application.service.token.TokenService;
import cmc.delta.domain.auth.model.SocialProvider;
import cmc.delta.domain.auth.adapter.out.persistence.SocialUserProvisionCommand;
import cmc.delta.domain.auth.adapter.out.persistence.UserProvisioningService;
import cmc.delta.global.config.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 소셜 로그인 플로우를 오케스트레이션. */
@Service
@RequiredArgsConstructor
public class SocialAuthFacade {

	// TODO: 실서비스는 DB 조회/Role 정책으로 교체
	private static final String DEFAULT_ROLE_FOR_DEV = "USER";

	private final SocialOAuthService socialOAuthFlowService;
	private final UserProvisioningService userProvisioningService;
	private final TokenService tokenService;

	public LoginResult loginWithCode(String code) {
		SocialOAuthService.SocialUserInfo userInfo = socialOAuthFlowService.fetchUserInfoByCode(code);

		UserProvisioningService.ProvisioningResult provisioned = userProvisioningService.provisionSocialUser(
			new SocialUserProvisionCommand(
				SocialProvider.KAKAO,
				userInfo.providerUserId(),
				userInfo.email(),
				userInfo.nickname()
			)
		);

		long userId = provisioned.user().getId();
		boolean isNewUser = provisioned.isNewUser();

		TokenIssuer.IssuedTokens tokens = tokenService.issue(new UserPrincipal(userId, DEFAULT_ROLE_FOR_DEV));

		return new LoginResult(new SocialLoginData(userInfo.email(), userInfo.nickname(), isNewUser), tokens);
	}

	public record LoginResult(SocialLoginData data, TokenIssuer.IssuedTokens tokens) {
	}
}
