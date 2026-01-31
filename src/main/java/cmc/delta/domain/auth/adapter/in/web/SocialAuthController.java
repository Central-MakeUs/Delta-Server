package cmc.delta.domain.auth.adapter.in.web;

import cmc.delta.domain.auth.adapter.in.support.TokenHeaderWriter;
import cmc.delta.domain.auth.adapter.in.web.dto.request.KakaoLoginRequest;
import cmc.delta.domain.auth.adapter.out.oauth.redis.RedisLoginKeyStore;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginCommandUseCase;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginData;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
import cmc.delta.global.config.swagger.AuthApiDocs;
import cmc.delta.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증")
@RestController
@RequestMapping("/api/v1/auth")
public class SocialAuthController {

	private final SocialLoginCommandUseCase socialLoginCommandUseCase;
	private final TokenHeaderWriter tokenHeaderWriter;

	@Operation(summary = "카카오 인가코드로 로그인", description = AuthApiDocs.KAKAO_LOGIN)
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.AUTHENTICATION_FAILED
	})
	@PostMapping("/kakao")
	public ApiResponse<SocialLoginData> kakao(@Valid @RequestBody
	KakaoLoginRequest request, HttpServletResponse response) {
		SocialLoginCommandUseCase.LoginResult result = socialLoginCommandUseCase.loginKakao(request.code());
		tokenHeaderWriter.write(response, result.tokens());
		return ApiResponses.success(SuccessCode.OK, result.data());
	}

	private final RedisLoginKeyStore loginKeyStore;

	public SocialAuthController(SocialLoginCommandUseCase socialLoginCommandUseCase,
		TokenHeaderWriter tokenHeaderWriter,
		RedisLoginKeyStore loginKeyStore) {
		this.socialLoginCommandUseCase = socialLoginCommandUseCase;
		this.tokenHeaderWriter = tokenHeaderWriter;
		this.loginKeyStore = loginKeyStore;
	}

	@Operation(summary = "Apple form_post 콜백 처리 (로그인키 발급)", description = AuthApiDocs.APPLE_FORM_POST_CALLBACK)
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.AUTHENTICATION_FAILED
	})
	@PostMapping(value = "/apple", consumes = "application/x-www-form-urlencoded")
	public void callback(
		@RequestParam("code")
		String code,
		@RequestParam(value = "user", required = false)
		String userJson,
		HttpServletResponse response) {

		SocialLoginCommandUseCase.LoginResult result = socialLoginCommandUseCase.loginApple(code, userJson);

		String loginKey = java.util.UUID.randomUUID().toString();
		loginKeyStore.save(loginKey, result.data(), result.tokens(), java.time.Duration.ofSeconds(60));

		String redirect = "http://localhost:3000/oauth/apple/callback?loginKey=" + loginKey;
		response.setStatus(org.springframework.http.HttpStatus.SEE_OTHER.value());
		response.setHeader(org.springframework.http.HttpHeaders.LOCATION, redirect);
	}

	@Operation(summary = "loginKey 교환", description = AuthApiDocs.APPLE_EXCHANGE)
	@PostMapping("/apple/exchange")
	public ApiResponse<SocialLoginData> exchange(@RequestParam("loginKey")
	String loginKey, HttpServletResponse response) {
		RedisLoginKeyStore.Stored stored = loginKeyStore.consume(loginKey);
		if (stored == null) {
			throw new RuntimeException("invalid_or_expired_login_key");
		}
		tokenHeaderWriter.write(response, stored.tokens());
		return ApiResponses.success(SuccessCode.OK, stored.data());
	}
}
