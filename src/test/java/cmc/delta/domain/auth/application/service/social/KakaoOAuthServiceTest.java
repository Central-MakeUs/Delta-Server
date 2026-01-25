package cmc.delta.domain.auth.application.service.social;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.auth.application.port.out.SocialOAuthClient;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KakaoOAuthServiceTest {

	@Test
	@DisplayName("인가코드로 조회: 토큰 교환 후 프로필을 조회하고 userInfo를 반환")
	void fetchUserInfoByCode_ok() {
		SocialOAuthClient client = mock(SocialOAuthClient.class);
		when(client.exchangeCode("code")).thenReturn(new SocialOAuthClient.OAuthToken("at"));
		when(client.fetchProfile("at")).thenReturn(new SocialOAuthClient.OAuthProfile("pid", "e@e.com", "nick"));

		KakaoOAuthService sut = new KakaoOAuthService(client);

		KakaoOAuthService.SocialUserInfo out = sut.fetchUserInfoByCode("code");

		assertThat(out.providerUserId()).isEqualTo("pid");
		assertThat(out.email()).isEqualTo("e@e.com");
		assertThat(out.nickname()).isEqualTo("nick");
	}

	@Test
	@DisplayName("인가코드로 조회: 프로필 email이 비어있으면 INVALID_REQUEST")
	void fetchUserInfoByCode_whenEmailMissing_thenInvalidRequest() {
		SocialOAuthClient client = mock(SocialOAuthClient.class);
		when(client.exchangeCode("code")).thenReturn(new SocialOAuthClient.OAuthToken("at"));
		when(client.fetchProfile("at")).thenReturn(new SocialOAuthClient.OAuthProfile("pid", " ", "nick"));

		KakaoOAuthService sut = new KakaoOAuthService(client);

		BusinessException ex = catchThrowableOfType(
			() -> sut.fetchUserInfoByCode("code"),
			BusinessException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}
}
