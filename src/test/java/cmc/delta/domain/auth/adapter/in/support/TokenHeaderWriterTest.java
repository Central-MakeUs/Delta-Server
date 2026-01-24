package cmc.delta.domain.auth.adapter.in.support;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

class TokenHeaderWriterTest {

	private final TokenHeaderWriter writer = new TokenHeaderWriter();

	@Test
	@DisplayName("헤더 작성: Authorization/Expose 헤더를 설정한다")
	void write_setsAuthorizationAndExposeHeaders() {
		MockHttpServletResponse resp = new MockHttpServletResponse();
		TokenIssuer.IssuedTokens tokens = new TokenIssuer.IssuedTokens("access", "refresh", "Bearer");

		writer.write(resp, tokens);

		assertThat(resp.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer access");
		assertThat(resp.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
			.isEqualTo(AuthHeaderConstants.EXPOSE_HEADERS_VALUE);
	}

	@Test
	@DisplayName("헤더 작성: refresh가 blank면 X-Refresh-Token은 쓰지 않는다")
	void write_whenRefreshBlank_thenDoesNotSetRefreshHeader() {
		MockHttpServletResponse resp = new MockHttpServletResponse();
		TokenIssuer.IssuedTokens tokens = new TokenIssuer.IssuedTokens("access", "   ", "Bearer");

		writer.write(resp, tokens);

		assertThat(resp.getHeader(AuthHeaderConstants.REFRESH_TOKEN_HEADER)).isNull();
		assertThat(resp.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer access");
	}
}
