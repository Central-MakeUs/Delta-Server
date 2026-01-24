package cmc.delta.domain.auth.application.service.social;

import org.springframework.stereotype.Service;

import cmc.delta.domain.auth.adapter.in.web.dto.response.SocialLoginData;
import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.auth.application.port.in.provisioning.UserProvisioningUseCase;
import cmc.delta.domain.auth.application.port.out.SocialAccountRepositoryPort;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.auth.model.SocialProvider;
import cmc.delta.global.config.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialAuthFacade {

	private static final String DEFAULT_ROLE_FOR_DEV = "USER";

	private final KakaoOAuthService kakaoOAuthService;
	private final AppleOAuthService appleOAuthService;
	private final SocialAccountRepositoryPort socialAccountRepositoryPort;

	private final UserProvisioningUseCase userProvisioningUseCase;
	private final TokenIssuer tokenIssuer;

	public LoginResult loginKakao(String code) {
		KakaoOAuthService.SocialUserInfo userInfo = kakaoOAuthService.fetchUserInfoByCode(code);

		UserProvisioningUseCase.ProvisioningResult provisioned =
			userProvisioningUseCase.provisionSocialUser(
				new SocialUserProvisionCommand(
					SocialProvider.KAKAO,
					userInfo.providerUserId(),
					userInfo.email(),
					userInfo.nickname()
				)
			);

		long userId = provisioned.userId();
		boolean isNewUser = provisioned.isNewUser();

		TokenIssuer.IssuedTokens tokens = tokenIssuer.issue(new UserPrincipal(userId, DEFAULT_ROLE_FOR_DEV));
		SocialLoginData data = new SocialLoginData(userInfo.email(), userInfo.nickname(), isNewUser);
		return new LoginResult(data, tokens);
	}

	public LoginResult loginApple(String code, String userJson) {
		AppleOAuthService.AppleUserInfo userInfo = appleOAuthService.fetchUserInfoByCode(code, userJson);

		UserProvisioningUseCase.ProvisioningResult provisioned =
			userProvisioningUseCase.provisionSocialUser(
				new SocialUserProvisionCommand(
					SocialProvider.APPLE,
					userInfo.providerUserId(),
					userInfo.email(),
					userInfo.nickname()
				)
			);

		long userId = provisioned.userId();
		boolean isNewUser = provisioned.isNewUser();

		TokenIssuer.IssuedTokens tokens =
			tokenIssuer.issue(new UserPrincipal(userId, DEFAULT_ROLE_FOR_DEV));

		SocialLoginData data = new SocialLoginData(userInfo.email(), userInfo.nickname(), isNewUser);
		return new LoginResult(data, tokens);
	}

	public record LoginResult(SocialLoginData data, TokenIssuer.IssuedTokens tokens) {}
}
