package cmc.delta.global.config.security.jwt;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class BearerTokenResolverTest {

	private final BearerTokenResolver resolver = new BearerTokenResolver();

	@Test
	@DisplayName("resolveBearerToken: 헤더가 없으면 null")
	void resolveBearerToken_whenMissing_thenNull() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest();

		// when
		String token = resolver.resolveBearerToken(req);

		// then
		assertThat(token).isNull();
	}

	@Test
	@DisplayName("resolveBearerToken: 헤더가 blank면 null")
	void resolveBearerToken_whenBlank_thenNull() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.addHeader("Authorization", " ");

		// when
		String token = resolver.resolveBearerToken(req);

		// then
		assertThat(token).isNull();
	}

	@Test
	@DisplayName("resolveBearerToken: Bearer prefix가 아니면 null")
	void resolveBearerToken_whenNotBearer_thenNull() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.addHeader("Authorization", "Basic abc");

		// when
		String token = resolver.resolveBearerToken(req);

		// then
		assertThat(token).isNull();
	}

	@Test
	@DisplayName("resolveBearerToken: Bearer만 있고 토큰이 비어있으면 null")
	void resolveBearerToken_whenBearerButEmpty_thenNull() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.addHeader("Authorization", "Bearer   ");

		// when
		String token = resolver.resolveBearerToken(req);

		// then
		assertThat(token).isNull();
	}

	@Test
	@DisplayName("resolveBearerToken: Bearer 토큰이면 trim 후 토큰만 반환")
	void resolveBearerToken_whenBearer_thenReturnsToken() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.addHeader("Authorization", "Bearer  a.b.c  ");

		// when
		String token = resolver.resolveBearerToken(req);

		// then
		assertThat(token).isEqualTo("a.b.c");
	}
}
