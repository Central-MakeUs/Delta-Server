package cmc.delta.domain.auth.application.service.social;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.auth.adapter.out.oauth.kakao.KakaoOAuthClient;
import cmc.delta.domain.auth.application.port.out.SocialOAuthClient;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class KakaoOAuthServiceTest {

	@Test
	@DisplayName("인가코드로 조회: 토큰 교환 후 프로필을 조회하고 userInfo를 반환")
	void fetchUserInfoByCode_ok() {
		KakaoOAuthClient client = mock(KakaoOAuthClient.class);
		when(client.exchangeCode("code")).thenReturn(new SocialOAuthClient.OAuthToken("at"));
		when(client.fetchProfile("at")).thenReturn(new SocialOAuthClient.OAuthProfile("pid", "e@e.com", "nick"));

		KakaoOAuthService sut = new KakaoOAuthService(client);

		SocialUserInfo out = sut.fetchUserInfoByCode("code");

		assertThat(out.providerUserId()).isEqualTo("pid");
		assertThat(out.email()).isEqualTo("e@e.com");
		assertThat(out.nickname()).isEqualTo("nick");
	}

	@Test
	@DisplayName("인가코드로 조회: 프로필 email이 비어있으면 INVALID_REQUEST")
	void fetchUserInfoByCode_whenEmailMissing_thenInvalidRequest() {
		KakaoOAuthClient client = mock(KakaoOAuthClient.class);
		when(client.exchangeCode("code")).thenReturn(new SocialOAuthClient.OAuthToken("at"));
		when(client.fetchProfile("at")).thenReturn(new SocialOAuthClient.OAuthProfile("pid", " ", "nick"));

		KakaoOAuthService sut = new KakaoOAuthService(client);

		BusinessException ex = catchThrowableOfType(
			() -> sut.fetchUserInfoByCode("code"),
			BusinessException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("액세스 토큰으로 조회: 토큰 검증 후 프로필을 조회하고 userInfo를 반환")
	void fetchUserInfoByAccessToken_ok() {
		KakaoOAuthClient client = mock(KakaoOAuthClient.class);
		when(client.fetchProfile("at"))
			.thenReturn(new SocialOAuthClient.OAuthProfile("pid", "e@e.com", "nick"));
		KakaoOAuthService sut = new KakaoOAuthService(client);

		SocialUserInfo out = sut.fetchUserInfoByAccessToken("at");

		InOrder inOrder = inOrder(client);
		inOrder.verify(client).validateAccessToken("at");
		inOrder.verify(client).fetchProfile("at");
		assertThat(out).isEqualTo(new SocialUserInfo("pid", "e@e.com", "nick"));
	}

	@Test
	@DisplayName("액세스 토큰으로 조회: 토큰 검증에 실패하면 프로필을 조회하지 않음")
	void fetchUserInfoByAccessToken_whenValidationFails_thenDoesNotFetchProfile() {
		KakaoOAuthClient client = mock(KakaoOAuthClient.class);
		BusinessException validationFailure = new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
		doThrow(validationFailure).when(client).validateAccessToken("invalid");
		KakaoOAuthService sut = new KakaoOAuthService(client);

		BusinessException ex = catchThrowableOfType(
			() -> sut.fetchUserInfoByAccessToken("invalid"),
			BusinessException.class);

		assertThat(ex).isSameAs(validationFailure);
		verify(client, never()).fetchProfile(anyString());
	}
}
