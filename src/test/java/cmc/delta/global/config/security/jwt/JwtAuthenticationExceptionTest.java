package cmc.delta.global.config.security.jwt;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtAuthenticationExceptionTest {

	@Test
	@DisplayName("JwtAuthenticationException: ErrorCode를 보존하고 기본 메시지를 사용")
	void ctor_whenErrorCode_thenKeepsErrorCodeAndMessage() {
		// given
		ErrorCode errorCode = ErrorCode.INVALID_TOKEN;

		// when
		JwtAuthenticationException ex = new JwtAuthenticationException(errorCode);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(errorCode);
		assertThat(ex.getMessage()).isEqualTo(errorCode.defaultMessage());
	}
}
