package cmc.delta.domain.auth.adapter.in.support;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.auth.application.exception.TokenException;
import cmc.delta.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class HttpTokenExtractorTest {

	private final HttpTokenExtractor extractor = new HttpTokenExtractor();

	@Test
	@DisplayName("refresh 추출: 헤더가 있으면 trim 후 반환")
	void extractRefreshToken_whenPresent_thenReturnsTrimmed() {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.addHeader(AuthHeaderConstants.REFRESH_TOKEN_HEADER, "  r  ");

		assertThat(extractor.extractRefreshToken(req)).isEqualTo("r");
	}

	@Test
	@DisplayName("refresh 추출: 헤더가 비어있으면 REFRESH_TOKEN_REQUIRED")
	void extractRefreshToken_whenMissing_thenThrows() {
		MockHttpServletRequest req = new MockHttpServletRequest();

		TokenException ex = catchThrowableOfType(() -> extractor.extractRefreshToken(req), TokenException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_REQUIRED);
	}

	@Test
	@DisplayName("access 추출: Authorization이 Bearer면 토큰만 반환")
	void extractAccessToken_whenBearer_thenReturnsToken() {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.addHeader(AuthHeaderConstants.AUTHORIZATION_HEADER, "Bearer  a  ");

		assertThat(extractor.extractAccessToken(req)).isEqualTo("a");
	}

	@Test
	@DisplayName("access 추출: Authorization이 없거나 Bearer가 아니면 TOKEN_REQUIRED")
	void extractAccessToken_whenInvalid_thenThrows() {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.addHeader(AuthHeaderConstants.AUTHORIZATION_HEADER, "Basic x");

		TokenException ex = catchThrowableOfType(() -> extractor.extractAccessToken(req), TokenException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TOKEN_REQUIRED);
	}
}
