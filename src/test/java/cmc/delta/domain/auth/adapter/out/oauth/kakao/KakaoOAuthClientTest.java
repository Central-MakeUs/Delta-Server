package cmc.delta.domain.auth.adapter.out.oauth.kakao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.auth.adapter.out.oauth.client.OAuthClientException;
import cmc.delta.domain.auth.adapter.out.oauth.client.OAuthHttpClient;
import cmc.delta.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;

class KakaoOAuthClientTest {

	private static final String TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info";

	@Test
	@DisplayName("액세스 토큰 검증: 공식 토큰 정보 API에 Bearer 토큰을 전달")
	void validateAccessToken_ok_usesBearerToken() {
		OAuthHttpClient httpClient = mock(OAuthHttpClient.class);
		KakaoOAuthClient client = new KakaoOAuthClient(properties(), httpClient);
		when(httpClient.get(
			eq("kakao"),
			eq("액세스 토큰 검증"),
			eq(TOKEN_INFO_URL),
			any(HttpHeaders.class),
			eq(KakaoAccessTokenInfoResponse.class)))
			.thenReturn(new KakaoAccessTokenInfoResponse(123L, 3600L, 999L));

		client.validateAccessToken("provider-access-token");

		ArgumentCaptor<HttpHeaders> headersCaptor = ArgumentCaptor.forClass(HttpHeaders.class);
		verify(httpClient).get(
			eq("kakao"),
			eq("액세스 토큰 검증"),
			eq(TOKEN_INFO_URL),
			headersCaptor.capture(),
			eq(KakaoAccessTokenInfoResponse.class));
		assertThat(headersCaptor.getValue().getFirst(HttpHeaders.AUTHORIZATION))
			.isEqualTo("Bearer provider-access-token");
	}

	@Test
	@DisplayName("액세스 토큰 검증: 응답이 비어있으면 OAUTH_INVALID_RESPONSE")
	void validateAccessToken_whenResponseMissing_thenThrowsInvalidResponse() {
		OAuthHttpClient httpClient = mock(OAuthHttpClient.class);
		KakaoOAuthClient client = new KakaoOAuthClient(properties(), httpClient);
		when(httpClient.get(anyString(), anyString(), anyString(), any(), eq(KakaoAccessTokenInfoResponse.class)))
			.thenReturn(null);

		OAuthClientException ex = catchThrowableOfType(
			() -> client.validateAccessToken("provider-access-token"),
			OAuthClientException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OAUTH_INVALID_RESPONSE);
	}

	@Test
	@DisplayName("액세스 토큰 검증: 만료 시간이 유효하지 않으면 OAUTH_INVALID_RESPONSE")
	void validateAccessToken_whenExpiresInInvalid_thenThrowsInvalidResponse() {
		OAuthHttpClient httpClient = mock(OAuthHttpClient.class);
		KakaoOAuthClient client = new KakaoOAuthClient(properties(), httpClient);
		when(httpClient.get(anyString(), anyString(), anyString(), any(), eq(KakaoAccessTokenInfoResponse.class)))
			.thenReturn(new KakaoAccessTokenInfoResponse(123L, 0L, 999L));

		OAuthClientException ex = catchThrowableOfType(
			() -> client.validateAccessToken("provider-access-token"),
			OAuthClientException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OAUTH_INVALID_RESPONSE);
	}

	private KakaoOAuthProperties properties() {
		return new KakaoOAuthProperties("client-id", "secret", "http://redirect", 1000, 1000);
	}
}
