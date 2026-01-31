package cmc.delta.domain.auth.adapter.in.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cmc.delta.domain.auth.adapter.in.support.TokenHeaderWriter;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginCommandUseCase;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginData;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SocialAuthControllerWebMvcTest {

	private MockMvc mvc;

	private SocialLoginCommandUseCase socialLoginCommandUseCase;
	private TokenHeaderWriter tokenHeaderWriter;
	private cmc.delta.domain.auth.adapter.out.oauth.redis.RedisLoginKeyStore redisLoginKeyStore;

	@BeforeEach
	void setUp() {
		socialLoginCommandUseCase = mock(SocialLoginCommandUseCase.class);
		tokenHeaderWriter = mock(TokenHeaderWriter.class);
		redisLoginKeyStore = mock(cmc.delta.domain.auth.adapter.out.oauth.redis.RedisLoginKeyStore.class);

		SocialAuthController controller = new SocialAuthController(socialLoginCommandUseCase, tokenHeaderWriter,
			redisLoginKeyStore);

		mvc = MockMvcBuilders.standaloneSetup(controller)
			.setMessageConverters(new MappingJackson2HttpMessageConverter())
			.build();
	}

	@Test
	@DisplayName("POST /auth/kakao: JSON 바인딩(code) + usecase 호출 + 토큰 헤더 작성")
	void kakao_ok_bindsBody() throws Exception {
		TokenIssuer.IssuedTokens tokens = new TokenIssuer.IssuedTokens("a", "r", "Bearer");
		when(socialLoginCommandUseCase.loginKakao("code"))
			.thenReturn(
				new SocialLoginCommandUseCase.LoginResult(new SocialLoginData("e@e.com", "nick", true), tokens));

		mvc.perform(post("/api/v1/auth/kakao")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"code\"}"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(socialLoginCommandUseCase).loginKakao("code");
		verify(tokenHeaderWriter).write(any(), eq(tokens));
	}

	@Test
	@DisplayName("POST /auth/apple: form(code/user) 바인딩 + usecase 호출 + 리다이렉트(303)")
	void apple_ok_bindsParams() throws Exception {
		TokenIssuer.IssuedTokens tokens = new TokenIssuer.IssuedTokens("a", "r", "Bearer");
		when(socialLoginCommandUseCase.loginApple("code", "user"))
			.thenReturn(
				new SocialLoginCommandUseCase.LoginResult(new SocialLoginData("e@e.com", "nick", false), tokens));

		mvc.perform(post("/api/v1/auth/apple")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("code", "code")
			.param("user", "user"))
			.andExpect(status().isSeeOther())
			.andExpect(header().string("Location",
				org.hamcrest.Matchers.containsString("http://localhost:3000/oauth/apple/callback?loginKey=")));

		verify(socialLoginCommandUseCase).loginApple("code", "user");
		verify(redisLoginKeyStore).save(anyString(), any(), eq(tokens), any());
	}

	@Test
	@DisplayName("POST /auth/apple/exchange: loginKey 교환 -> 토큰 헤더 작성 + JSON 반환")
	void apple_exchange_ok() throws Exception {
		TokenIssuer.IssuedTokens tokens = new TokenIssuer.IssuedTokens("a", "r", "Bearer");
		when(redisLoginKeyStore.consume("key"))
			.thenReturn(new cmc.delta.domain.auth.adapter.out.oauth.redis.RedisLoginKeyStore.Stored(
				new SocialLoginData("e@e.com", "nick", false), tokens));

		mvc.perform(post("/api/v1/auth/apple/exchange")
			.param("loginKey", "key"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(tokenHeaderWriter).write(any(), eq(tokens));
	}
}
