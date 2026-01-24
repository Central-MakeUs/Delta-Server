package cmc.delta.domain.auth.application.service.social;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.auth.application.port.in.social.SocialLoginCommandUseCase;
import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.auth.application.port.in.provisioning.UserProvisioningUseCase;
import cmc.delta.domain.auth.application.port.in.token.TokenCommandUseCase;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.auth.model.SocialProvider;
import cmc.delta.global.config.security.principal.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SocialAuthFacadeTest {

	@Test
	@DisplayName("카카오 로그인: 유저정보→프로비저닝→토큰발급 후 LoginResult를 반환")
	void loginKakao_ok() {
		KakaoOAuthService kakao = mock(KakaoOAuthService.class);
		AppleOAuthService apple = mock(AppleOAuthService.class);
		UserProvisioningUseCase provisioning = mock(UserProvisioningUseCase.class);
		TokenCommandUseCase tokenUseCase = mock(TokenCommandUseCase.class);

		when(kakao.fetchUserInfoByCode("code"))
			.thenReturn(new KakaoOAuthService.SocialUserInfo("pid", "e@e.com", "nick"));
		when(provisioning.provisionSocialUser(any()))
			.thenReturn(new UserProvisioningUseCase.ProvisioningResult(7L, "e@e.com", "nick", true));
		TokenIssuer.IssuedTokens tokens = new TokenIssuer.IssuedTokens("a", "r", "Bearer");
		when(tokenUseCase.issue(any())).thenReturn(tokens);

		SocialAuthFacade sut = new SocialAuthFacade(kakao, apple, provisioning, tokenUseCase);

		SocialLoginCommandUseCase.LoginResult out = sut.loginKakao("code");

		ArgumentCaptor<SocialUserProvisionCommand> cmdCaptor = ArgumentCaptor.forClass(SocialUserProvisionCommand.class);
		verify(provisioning).provisionSocialUser(cmdCaptor.capture());
		assertThat(cmdCaptor.getValue())
			.isEqualTo(new SocialUserProvisionCommand(SocialProvider.KAKAO, "pid", "e@e.com", "nick"));

		ArgumentCaptor<UserPrincipal> principalCaptor = ArgumentCaptor.forClass(UserPrincipal.class);
		verify(tokenUseCase).issue(principalCaptor.capture());
		assertThat(principalCaptor.getValue().userId()).isEqualTo(7L);
		assertThat(principalCaptor.getValue().role()).isEqualTo("USER");

		assertThat(out.tokens()).isEqualTo(tokens);
		assertThat(out.data().email()).isEqualTo("e@e.com");
		assertThat(out.data().nickname()).isEqualTo("nick");
		assertThat(out.data().isNewUser()).isTrue();
	}

	@Test
	@DisplayName("애플 로그인: code/userJson을 애플 서비스에 전달하고 LoginResult를 반환")
	void loginApple_ok() {
		KakaoOAuthService kakao = mock(KakaoOAuthService.class);
		AppleOAuthService apple = mock(AppleOAuthService.class);
		UserProvisioningUseCase provisioning = mock(UserProvisioningUseCase.class);
		TokenCommandUseCase tokenUseCase = mock(TokenCommandUseCase.class);

		when(apple.fetchUserInfoByCode("code", "user"))
			.thenReturn(new AppleOAuthService.AppleUserInfo("sub", "e@e.com", "nick"));
		when(provisioning.provisionSocialUser(any()))
			.thenReturn(new UserProvisioningUseCase.ProvisioningResult(7L, "e@e.com", "nick", false));
		TokenIssuer.IssuedTokens tokens = new TokenIssuer.IssuedTokens("a", "r", "Bearer");
		when(tokenUseCase.issue(any())).thenReturn(tokens);

		SocialAuthFacade sut = new SocialAuthFacade(kakao, apple, provisioning, tokenUseCase);

		SocialLoginCommandUseCase.LoginResult out = sut.loginApple("code", "user");

		verify(apple).fetchUserInfoByCode("code", "user");
		assertThat(out.data().isNewUser()).isFalse();
	}
}
