package cmc.delta.domain.auth.application.service.social;

import org.springframework.stereotype.Service;

import cmc.delta.domain.auth.application.port.in.social.SocialLoginData;
import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.auth.application.port.in.provisioning.UserProvisioningUseCase;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginCommandUseCase;
import cmc.delta.domain.auth.application.port.in.token.TokenCommandUseCase;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.auth.model.SocialProvider;
import cmc.delta.domain.auth.application.support.AuthRoleDefaults;
import cmc.delta.global.config.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialAuthFacade implements SocialLoginCommandUseCase {

	private final KakaoOAuthService kakaoOAuthService;
	private final AppleOAuthService appleOAuthService;

	private final UserProvisioningUseCase userProvisioningUseCase;
	private final TokenCommandUseCase tokenCommandUseCase;

	@Override
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
		TokenIssuer.IssuedTokens tokens = tokenCommandUseCase.issue(principalOf(provisioned.userId()));
		SocialLoginData data = new SocialLoginData(provisioned.email(), provisioned.nickname(), provisioned.isNewUser());
		return new LoginResult(data, tokens);
	}

	@Override
	public LoginResult loginApple(String code, String userJson) {
		AppleOAuthService.AppleUserInfo apple = appleOAuthService.fetchUserInfoByCode(code, userJson);
		String providerUserId = apple.providerUserId(); // sub

		UserProvisioningUseCase.ProvisioningResult provisioned =
			userProvisioningUseCase.provisionSocialUser(
				new SocialUserProvisionCommand(
					SocialProvider.APPLE,
					providerUserId,
					apple.email(),
					apple.nickname()
				)
			);


		TokenIssuer.IssuedTokens tokens = tokenCommandUseCase.issue(principalOf(provisioned.userId()));
		SocialLoginData data = new SocialLoginData(provisioned.email(), provisioned.nickname(), provisioned.isNewUser());
		return new LoginResult(data, tokens);
	}

	private UserPrincipal principalOf(long userId) {
		return new UserPrincipal(userId, AuthRoleDefaults.DEFAULT_ROLE_FOR_DEV);
	}
}
