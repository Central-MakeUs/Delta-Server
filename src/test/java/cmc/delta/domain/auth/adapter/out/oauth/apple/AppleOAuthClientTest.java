package cmc.delta.domain.auth.adapter.out.oauth.apple;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

class AppleOAuthClientTest {

	private static final String CLIENT_ID = "client-id";
	private static final String TEAM_ID = "team-id";
	private static final String KEY_ID = "key-id";
	private static final String REDIRECT_URI = "https://example.com/callback";

	@Test
	@DisplayName("code가 비어있으면 INVALID_REQUEST")
	void exchangeCode_whenCodeBlank_thenInvalidRequest() {
		AppleOAuthProperties props = new AppleOAuthProperties(
			CLIENT_ID,
			TEAM_ID,
			KEY_ID,
			buildEcPrivateKeyPem(),
			REDIRECT_URI,
			1000,
			1000);
		RestTemplate restTemplate = mock(RestTemplate.class);
		AppleOAuthClient client = new AppleOAuthClient(props, restTemplate);

		BusinessException ex = catchThrowableOfType(
			() -> client.exchangeCode(" "),
			BusinessException.class);
		assertThat(ex).isInstanceOf(AppleOAuthException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("토큰 교환 응답이 비어있으면 OAUTH_TOKEN_EXCHANGE_INVALID_RESPONSE")
	void exchangeCode_whenTokenResponseInvalid_thenMapped() {
		AppleOAuthProperties props = new AppleOAuthProperties(
			CLIENT_ID,
			TEAM_ID,
			KEY_ID,
			buildEcPrivateKeyPem(),
			REDIRECT_URI,
			1000,
			1000);
		RestTemplate restTemplate = mock(RestTemplate.class);
		AppleOAuthClient client = new AppleOAuthClient(props, restTemplate);

		AppleOAuthClient.AppleTokenResponse body = new AppleOAuthClient.AppleTokenResponse(
			"access",
			"bearer",
			3600L,
			"refresh",
			null);
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(AppleOAuthClient.AppleTokenResponse.class)))
			.thenReturn(ResponseEntity.ok(body));

		BusinessException ex = catchThrowableOfType(
			() -> client.exchangeCode("code"),
			BusinessException.class);
		assertThat(ex).isInstanceOf(AppleOAuthException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OAUTH_TOKEN_EXCHANGE_INVALID_RESPONSE);
	}

	@Test
	@DisplayName("토큰 교환 5xx면 OAUTH_TOKEN_EXCHANGE_FAILED")
	void exchangeCode_when5xx_thenMapped() {
		AppleOAuthProperties props = new AppleOAuthProperties(
			CLIENT_ID,
			TEAM_ID,
			KEY_ID,
			buildEcPrivateKeyPem(),
			REDIRECT_URI,
			1000,
			1000);
		RestTemplate restTemplate = mock(RestTemplate.class);
		AppleOAuthClient client = new AppleOAuthClient(props, restTemplate);

		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(AppleOAuthClient.AppleTokenResponse.class)))
			.thenThrow(new HttpServerErrorException(org.springframework.http.HttpStatus.BAD_GATEWAY));

		BusinessException ex = catchThrowableOfType(
			() -> client.exchangeCode("code"),
			BusinessException.class);
		assertThat(ex).isInstanceOf(AppleOAuthException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED);
	}

	@Test
	@DisplayName("토큰 교환 타임아웃이면 OAUTH_TOKEN_EXCHANGE_TIMEOUT")
	void exchangeCode_whenTimeout_thenMapped() {
		AppleOAuthProperties props = new AppleOAuthProperties(
			CLIENT_ID,
			TEAM_ID,
			KEY_ID,
			buildEcPrivateKeyPem(),
			REDIRECT_URI,
			1000,
			1000);
		RestTemplate restTemplate = mock(RestTemplate.class);
		AppleOAuthClient client = new AppleOAuthClient(props, restTemplate);

		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(AppleOAuthClient.AppleTokenResponse.class)))
			.thenThrow(new ResourceAccessException("timeout"));

		BusinessException ex = catchThrowableOfType(
			() -> client.exchangeCode("code"),
			BusinessException.class);
		assertThat(ex).isInstanceOf(AppleOAuthException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OAUTH_TOKEN_EXCHANGE_TIMEOUT);
	}

	private String buildEcPrivateKeyPem() {
		try {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
			gen.initialize(new ECGenParameterSpec("secp256r1"));
			KeyPair kp = gen.generateKeyPair();
			String b64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
			return "-----BEGIN PRIVATE KEY-----\n" + b64 + "\n-----END PRIVATE KEY-----";
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
