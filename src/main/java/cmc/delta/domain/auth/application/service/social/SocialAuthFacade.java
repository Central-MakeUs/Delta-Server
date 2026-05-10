package cmc.delta.domain.auth.application.service.social;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.auth.application.port.in.provisioning.UserProvisioningUseCase;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginCommandUseCase;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginData;
import cmc.delta.domain.auth.application.port.in.token.TokenCommandUseCase;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.auth.model.SocialProvider;
import cmc.delta.global.config.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialAuthFacade implements SocialLoginCommandUseCase {

	private final KakaoOAuthService kakaoOAuthService;
	private final AppleOAuthService appleOAuthService;
	private final GoogleOAuthService googleOAuthService;

	private final UserProvisioningUseCase userProvisioningUseCase;
	private final TokenCommandUseCase tokenCommandUseCase;

	@Override
	public LoginResult loginKakao(String code) {
		SocialUserInfo userInfo = kakaoOAuthService.fetchUserInfoByCode(code);
		return loginWithProvisionedUser(SocialProvider.KAKAO, userInfo.providerUserId(), userInfo.email(),
			userInfo.nickname());
	}

	@Override
	public LoginResult loginApple(String code, String userJson) {
		SocialUserInfo userInfo = appleOAuthService.fetchUserInfoByCode(code, userJson);
		return loginWithProvisionedUser(SocialProvider.APPLE, userInfo.providerUserId(), userInfo.email(),
			userInfo.nickname());
	}

	@Override
	public LoginResult loginGoogle(String code) {
		SocialUserInfo userInfo = googleOAuthService.fetchUserInfoByCode(code);
		return loginWithProvisionedUser(SocialProvider.GOOGLE, userInfo.providerUserId(), userInfo.email(),
			userInfo.nickname());
	}

	private LoginResult loginWithProvisionedUser(
		SocialProvider provider,
		String providerUserId,
		String email,
		String nickname) {
		UserProvisioningUseCase.ProvisioningResult provisioned = userProvisioningUseCase.provisionSocialUser(
			new SocialUserProvisionCommand(provider, providerUserId, email, nickname));
		TokenIssuer.IssuedTokens tokens = tokenCommandUseCase.issue(
			new UserPrincipal(provisioned.userId(), provisioned.role().name()));
		SocialLoginData data = new SocialLoginData(
			provisioned.email(),
			provisioned.nickname(),
			provisioned.isNewUser());
		return new LoginResult(data, tokens);
	}

}
