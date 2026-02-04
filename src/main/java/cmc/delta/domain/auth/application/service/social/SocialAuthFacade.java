package cmc.delta.domain.auth.application.service.social;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.auth.application.port.in.provisioning.UserProvisioningUseCase;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginCommandUseCase;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginData;
import cmc.delta.domain.auth.application.port.in.token.TokenCommandUseCase;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.auth.application.support.AuthPrincipalFactory;
import cmc.delta.domain.auth.model.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
		return loginWithProvisionedUser(
			SocialProvider.KAKAO,
			userInfo.providerUserId(),
			userInfo.email(),
			userInfo.nickname());
	}

	@Override
	public LoginResult loginApple(String code, String userJson) {
		AppleOAuthService.AppleUserInfo apple = appleOAuthService.fetchUserInfoByCode(code, userJson);
		String providerUserId = apple.providerUserId(); // sub (토큰의 sub 클레임)
		return loginWithProvisionedUser(
			SocialProvider.APPLE,
			providerUserId,
			apple.email(),
			apple.nickname());
	}

	private LoginResult loginWithProvisionedUser(
		SocialProvider provider,
		String providerUserId,
		String email,
		String nickname) {
		UserProvisioningUseCase.ProvisioningResult provisioned = userProvisioningUseCase.provisionSocialUser(
			new SocialUserProvisionCommand(provider, providerUserId, email, nickname));
		TokenIssuer.IssuedTokens tokens = tokenCommandUseCase.issue(
			AuthPrincipalFactory.principalOf(provisioned.userId()));
		SocialLoginData data = new SocialLoginData(
			provisioned.email(),
			provisioned.nickname(),
			provisioned.isNewUser());
		return new LoginResult(data, tokens);
	}

}
