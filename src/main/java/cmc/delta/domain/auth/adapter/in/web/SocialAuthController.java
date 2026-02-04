package cmc.delta.domain.auth.adapter.in.web;

import cmc.delta.domain.auth.adapter.in.support.TokenHeaderWriter;
import cmc.delta.domain.auth.adapter.in.web.dto.request.KakaoLoginRequest;
import cmc.delta.domain.auth.adapter.out.oauth.redis.RedisLoginKeyStore;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginCommandUseCase;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginData;
import cmc.delta.domain.auth.application.service.social.SocialAuthService;
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
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class SocialAuthController {

	private static final long LOGIN_KEY_TTL_SECONDS = 60L;
	private static final Duration LOGIN_KEY_TTL = Duration.ofSeconds(LOGIN_KEY_TTL_SECONDS);
	private static final String APPLE_FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
	private static final String CODE_PARAM = "code";
	private static final String USER_PARAM = "user";
	private static final String LOGIN_KEY_PARAM = "loginKey";
	private static final String HEADER_LOCATION = org.springframework.http.HttpHeaders.LOCATION;
	private static final int SEE_OTHER = org.springframework.http.HttpStatus.SEE_OTHER.value();

	private final SocialLoginCommandUseCase socialLoginCommandUseCase;
	private final TokenHeaderWriter tokenHeaderWriter;
	private final SocialAuthService socialAuthService;

	@Operation(summary = "카카오 인가코드로 로그인", description = AuthApiDocs.KAKAO_LOGIN)
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.AUTHENTICATION_FAILED
	})
	@PostMapping("/kakao")
	public ApiResponse<SocialLoginData> kakao(
		@Valid
		@RequestBody
		KakaoLoginRequest request,
		HttpServletResponse response) {
		SocialLoginCommandUseCase.LoginResult result = socialLoginCommandUseCase.loginKakao(request.code());
		tokenHeaderWriter.write(response, result.tokens());
		return ApiResponses.success(SuccessCode.OK, result.data());
	}

	@Operation(summary = "Apple form_post 콜백 처리 (로그인키 발급)", description = AuthApiDocs.APPLE_FORM_POST_CALLBACK)
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.AUTHENTICATION_FAILED
	})
	@PostMapping(value = "/apple", consumes = APPLE_FORM_CONTENT_TYPE)
	public void callback(
		@RequestParam(CODE_PARAM)
		String code,
		@RequestParam(value = USER_PARAM, required = false)
		String userJson,
		HttpServletResponse response) {

		SocialLoginCommandUseCase.LoginResult result = socialLoginCommandUseCase.loginApple(code, userJson);

		String redirect = socialAuthService.createLoginKeyAndBuildRedirect(result, LOGIN_KEY_TTL);
		response.setStatus(SEE_OTHER);
		response.setHeader(HEADER_LOCATION, redirect);
	}

	@Operation(summary = "loginKey 교환", description = AuthApiDocs.APPLE_EXCHANGE)
	@PostMapping("/apple/exchange")
	public ApiResponse<SocialLoginData> exchange(
		@RequestParam(LOGIN_KEY_PARAM)
		String loginKey,
		HttpServletResponse response) {
		RedisLoginKeyStore.Stored stored = socialAuthService.consumeLoginKey(loginKey);
		tokenHeaderWriter.write(response, stored.tokens());
		return ApiResponses.success(SuccessCode.OK, stored.data());
	}
}
