package cmc.delta.domain.auth.application.service.social;

import java.util.Optional;

import org.springframework.stereotype.Service;

import cmc.delta.domain.auth.adapter.in.web.dto.response.SocialLoginData;
import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.auth.application.port.in.provisioning.UserProvisioningUseCase;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginUseCase;
import cmc.delta.domain.auth.application.port.out.SocialAccountRepositoryPort;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.auth.model.SocialAccount;
import cmc.delta.domain.auth.model.SocialProvider;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
	@RequiredArgsConstructor
public class SocialAuthFacade implements SocialLoginUseCase {

	private static final String DEFAULT_ROLE_FOR_DEV = "USER";

	private final KakaoOAuthService kakaoOAuthService;
	private final AppleOAuthService appleOAuthService;
	private final SocialAccountRepositoryPort socialAccountRepositoryPort;

	private final UserProvisioningUseCase userProvisioningUseCase;
	private final TokenIssuer tokenIssuer;

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

		long userId = provisioned.userId();
		boolean isNewUser = provisioned.isNewUser();

		TokenIssuer.IssuedTokens tokens = tokenIssuer.issue(new UserPrincipal(userId, DEFAULT_ROLE_FOR_DEV));
		SocialLoginData data = new SocialLoginData(userInfo.email(), userInfo.nickname(), isNewUser);
		return new LoginResult(data, tokens);
	}

	@Override
	public LoginResult loginApple(String code, String userJson) {
		AppleOAuthService.AppleUserInfo apple = appleOAuthService.fetchUserInfoByCode(code, userJson);
		String providerUserId = apple.providerUserId(); // sub

		Optional<SocialAccount> existing =
			socialAccountRepositoryPort.findByProviderAndProviderUserId(SocialProvider.APPLE, providerUserId);

		if (existing.isPresent()) {
			User user = existing.get().getUser();
			if (user.isWithdrawn()) {
				throw new BusinessException(ErrorCode.ACCESS_DENIED, "탈퇴한 사용자입니다.");
			}

			TokenIssuer.IssuedTokens tokens =
				tokenIssuer.issue(new UserPrincipal(user.getId(), DEFAULT_ROLE_FOR_DEV));

			SocialLoginData data =
				new SocialLoginData(user.getEmail(), user.getNickname(), false);

			return new LoginResult(data, tokens);
		}

		if (!hasText(apple.email())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "애플 이메일 제공 동의(최초 1회)가 필요합니다.");
		}
		if (!hasText(apple.nickname())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "애플 이름 제공 동의(최초 1회)가 필요합니다.");
		}

		UserProvisioningUseCase.ProvisioningResult provisioned =
			userProvisioningUseCase.provisionSocialUser(
				new SocialUserProvisionCommand(
					SocialProvider.APPLE,
					providerUserId,
					apple.email(),
					apple.nickname()
				)
			);

		long userId = provisioned.userId();
		boolean isNewUser = provisioned.isNewUser();

		TokenIssuer.IssuedTokens tokens =
			tokenIssuer.issue(new UserPrincipal(userId, DEFAULT_ROLE_FOR_DEV));

		SocialLoginData data = new SocialLoginData(apple.email(), apple.nickname(), isNewUser);
		return new LoginResult(data, tokens);
	}

	private boolean hasText(String s) {
		return s != null && !s.trim().isEmpty();
	}
}
