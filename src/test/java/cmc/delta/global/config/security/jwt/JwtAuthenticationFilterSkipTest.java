package cmc.delta.global.config.security.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.auth.application.port.out.AccessBlacklistStore;
import cmc.delta.global.config.security.SecurityConfig;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.AuthenticationEntryPoint;

class JwtAuthenticationFilterSkipTest {

	private JwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		JwtProperties properties = new JwtProperties(
			"delta-test-issuer",
			"dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2",
			900L,
			1209600L,
			new JwtProperties.Blacklist(false));

		filter = new JwtAuthenticationFilter(
			mock(BearerTokenResolver.class),
			mock(JwtTokenProvider.class),
			mock(AccessBlacklistStore.class),
			properties,
			mock(AuthenticationEntryPoint.class));
	}

	static Stream<String> skipPaths() {
		return Stream.of(
			"/api/v1/auth/kakao",
			"/api/v1/auth/google",
			"/api/v1/auth/apple",
			"/api/v1/auth/reissue",
			"/apple/callback");
	}

	@ParameterizedTest
	@MethodSource("skipPaths")
	@DisplayName("JWT_SKIP_PATHS에 해당하는 경로는 필터를 건너뜀")
	void shouldNotFilter_whenSkipPath_thenTrue(String path) throws Exception {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(path);

		// when
		boolean skipped = filter.shouldNotFilter(request);

		// then
		assertThat(skipped).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"/api/v1/problems",
		"/api/v1/users/me",
		"/api/v1/auth-other/something"
	})
	@DisplayName("JWT_SKIP_PATHS에 해당하지 않는 경로는 필터를 통과함")
	void shouldNotFilter_whenNonSkipPath_thenFalse(String path) throws Exception {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(path);

		// when
		boolean skipped = filter.shouldNotFilter(request);

		// then
		assertThat(skipped).isFalse();
	}

	@ParameterizedTest
	@MethodSource("skipPaths")
	@DisplayName("JWT_SKIP_PATHS 상수와 shouldNotFilter 결과가 일치함")
	void shouldNotFilter_matchesJwtSkipPathsConstant(String path) throws Exception {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(path);

		boolean expectedSkip = Stream.of(SecurityConfig.JWT_SKIP_PATHS)
			.anyMatch(path::startsWith);

		// when
		boolean actualSkip = filter.shouldNotFilter(request);

		// then
		assertThat(actualSkip).isEqualTo(expectedSkip);
	}
}
