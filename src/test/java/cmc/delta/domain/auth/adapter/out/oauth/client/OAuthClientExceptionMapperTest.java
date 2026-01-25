package cmc.delta.domain.auth.adapter.out.oauth.client;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import cmc.delta.global.error.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

class OAuthClientExceptionMapperTest {

	private final OAuthClientExceptionMapper mapper = new OAuthClientExceptionMapper();

	@Test
	@DisplayName("4xx면 UnauthorizedException으로 매핑")
	void mapHttpStatus_when4xx_thenUnauthorized() {
		RuntimeException ex = mapper.mapHttpStatus("kakao", "token", new HttpClientErrorException(HttpStatus.BAD_REQUEST));
		assertThat(ex).isInstanceOf(UnauthorizedException.class);
	}

	@Test
	@DisplayName("5xx면 OAUTH_PROVIDER_ERROR로 매핑")
	void mapHttpStatus_when5xx_thenInternalError() {
		RuntimeException ex = mapper.mapHttpStatus("kakao", "token", new HttpServerErrorException(HttpStatus.BAD_GATEWAY));
		assertThat(ex).isInstanceOf(OAuthClientException.class);
		assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.OAUTH_PROVIDER_ERROR);
	}
}
