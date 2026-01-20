package cmc.delta.domain.auth.application.service.social;

import cmc.delta.domain.auth.adapter.in.web.dto.response.SocialLoginData;
import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.auth.application.port.in.provisioning.UserProvisioningUseCase;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.auth.application.service.token.TokenService;
import cmc.delta.domain.auth.model.SocialProvider;
import cmc.delta.global.config.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialAuthFacade {

	private static final String DEFAULT_ROLE_FOR_DEV = "USER";

	private final SocialOAuthService socialOAuthFlowService;
	private final UserProvisioningUseCase userProvisioningUseCase;
	private final TokenService tokenService;

	public LoginResult loginWithCode(String code) {
		SocialOAuthService.SocialUserInfo userInfo = socialOAuthFlowService.fetchUserInfoByCode(code);

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

		TokenIssuer.IssuedTokens tokens =
			tokenService.issue(new UserPrincipal(userId, DEFAULT_ROLE_FOR_DEV));

		SocialLoginData data = new SocialLoginData(userInfo.email(), userInfo.nickname(), isNewUser);
		return new LoginResult(data, tokens);
	}

	public record LoginResult(SocialLoginData data, TokenIssuer.IssuedTokens tokens) {}
}
