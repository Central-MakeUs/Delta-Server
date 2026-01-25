package cmc.delta.domain.auth.application.service.social;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.auth.adapter.out.oauth.apple.AppleIdTokenVerifier;
import cmc.delta.domain.auth.adapter.out.oauth.apple.AppleOAuthClient;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppleOAuthServiceTest {

	@Test
	@DisplayName("애플 로그인: sub/email을 구성하고 form user name을 nickname으로 합친다")
	void fetchUserInfoByCode_ok_buildsNickname() {
		AppleOAuthClient client = mock(AppleOAuthClient.class);
		AppleIdTokenVerifier verifier = mock(AppleIdTokenVerifier.class);
		ObjectMapper objectMapper = new ObjectMapper();

		when(client.exchangeCode("code"))
			.thenReturn(new AppleOAuthClient.AppleTokenResponse(null, null, null, null, "id"));
		when(verifier.verifyAndExtract("id")).thenReturn(new AppleIdTokenVerifier.AppleIdClaims("sub", "v@e.com"));

		AppleOAuthService sut = new AppleOAuthService(client, verifier, objectMapper);

		String userJson = "{\"email\":\"form@e.com\",\"name\":{\"firstName\":\"Gil\",\"lastName\":\"Hong\"}}";
		AppleOAuthService.AppleUserInfo out = sut.fetchUserInfoByCode("code", userJson);

		assertThat(out.providerUserId()).isEqualTo("sub");
		assertThat(out.email()).isEqualTo("form@e.com");
		assertThat(out.nickname()).isEqualTo("HongGil");
	}

	@Test
	@DisplayName("애플 로그인: userJson이 잘못되면 INVALID_REQUEST")
	void fetchUserInfoByCode_whenUserJsonInvalid_thenInvalidRequest() {
		AppleOAuthClient client = mock(AppleOAuthClient.class);
		AppleIdTokenVerifier verifier = mock(AppleIdTokenVerifier.class);
		ObjectMapper objectMapper = new ObjectMapper();

		when(client.exchangeCode("code"))
			.thenReturn(new AppleOAuthClient.AppleTokenResponse(null, null, null, null, "id"));
		when(verifier.verifyAndExtract("id")).thenReturn(new AppleIdTokenVerifier.AppleIdClaims("sub", "v@e.com"));

		AppleOAuthService sut = new AppleOAuthService(client, verifier, objectMapper);

		BusinessException ex = catchThrowableOfType(
			() -> sut.fetchUserInfoByCode("code", "not-json"),
			BusinessException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("애플 로그인: sub가 비어있으면 AUTHENTICATION_FAILED")
	void fetchUserInfoByCode_whenSubBlank_thenAuthFailed() {
		AppleOAuthClient client = mock(AppleOAuthClient.class);
		AppleIdTokenVerifier verifier = mock(AppleIdTokenVerifier.class);
		ObjectMapper objectMapper = new ObjectMapper();

		when(client.exchangeCode("code"))
			.thenReturn(new AppleOAuthClient.AppleTokenResponse(null, null, null, null, "id"));
		when(verifier.verifyAndExtract("id")).thenReturn(new AppleIdTokenVerifier.AppleIdClaims(" ", "v@e.com"));

		AppleOAuthService sut = new AppleOAuthService(client, verifier, objectMapper);

		BusinessException ex = catchThrowableOfType(
			() -> sut.fetchUserInfoByCode("code", null),
			BusinessException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
	}
}
