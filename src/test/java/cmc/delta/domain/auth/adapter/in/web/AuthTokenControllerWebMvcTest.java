package cmc.delta.domain.auth.adapter.in.web;

import static cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver.ATTR;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cmc.delta.domain.auth.adapter.in.support.HttpTokenExtractor;
import cmc.delta.domain.auth.adapter.in.support.TokenHeaderWriter;
import cmc.delta.domain.auth.application.port.in.token.TokenCommandUseCase;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.problem.adapter.in.web.TestCurrentUserArgumentResolver;
import cmc.delta.global.config.security.principal.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthTokenControllerWebMvcTest {

	private MockMvc mvc;

	private TokenCommandUseCase tokenCommandUseCase;
	private HttpTokenExtractor httpTokenExtractor;
	private TokenHeaderWriter tokenHeaderWriter;

	@BeforeEach
	void setUp() {
		tokenCommandUseCase = mock(TokenCommandUseCase.class);
		httpTokenExtractor = mock(HttpTokenExtractor.class);
		tokenHeaderWriter = mock(TokenHeaderWriter.class);

		AuthTokenController controller = new AuthTokenController(tokenCommandUseCase, httpTokenExtractor,
			tokenHeaderWriter);

		mvc = MockMvcBuilders.standaloneSetup(controller)
			.setCustomArgumentResolvers(new TestCurrentUserArgumentResolver())
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.build();
	}

	@Test
	@DisplayName("POST /auth/reissue: refresh 추출 + usecase 호출 + 토큰 헤더 작성")
	void reissue_ok_callsUseCase() throws Exception {
		when(httpTokenExtractor.extractRefreshToken(any())).thenReturn("r");
		TokenIssuer.IssuedTokens tokens = new TokenIssuer.IssuedTokens("a", "r2", "Bearer");
		when(tokenCommandUseCase.reissue("r")).thenReturn(tokens);

		mvc.perform(post("/api/v1/auth/reissue"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(tokenCommandUseCase).reissue("r");
		verify(tokenHeaderWriter).write(any(), eq(tokens));
	}

	@Test
	@DisplayName("POST /auth/logout: access 추출 + invalidateAll 호출")
	void logout_ok_callsInvalidateAll() throws Exception {
		when(httpTokenExtractor.extractAccessToken(any())).thenReturn("a");
		UserPrincipal principal = principal(10L);

		mvc.perform(post("/api/v1/auth/logout")
				.requestAttr(ATTR, principal))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(tokenCommandUseCase).invalidateAll(10L, "a");
	}

	private UserPrincipal principal(long userId) {
		UserPrincipal p = mock(UserPrincipal.class);
		when(p.userId()).thenReturn(userId);
		return p;
	}
}
