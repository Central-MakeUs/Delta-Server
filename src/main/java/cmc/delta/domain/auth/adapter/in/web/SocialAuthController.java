package cmc.delta.domain.auth.adapter.in.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cmc.delta.domain.auth.adapter.in.support.TokenHeaderWriter;
import cmc.delta.domain.auth.adapter.in.web.dto.request.KakaoLoginRequest;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginUseCase;
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
import lombok.RequiredArgsConstructor;

@Tag(name = "인증")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class SocialAuthController {

	private final SocialLoginUseCase socialLoginUseCase;
	private final TokenHeaderWriter tokenHeaderWriter;

	@Operation(
		summary = "카카오 인가코드로 로그인",
		description = AuthApiDocs.KAKAO_LOGIN
	)
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.AUTHENTICATION_FAILED
	})
	@PostMapping("/kakao")
	public ApiResponse<SocialLoginData> kakao(
		@Valid @RequestBody KakaoLoginRequest request,
		HttpServletResponse response
	) {
		SocialLoginUseCase.LoginResult result = socialLoginUseCase.loginKakao(request.code());
		tokenHeaderWriter.write(response, result.tokens());
		return ApiResponses.success(SuccessCode.OK, result.data());
	}

	@Operation(
		summary = "애플 콜백(form_post) 처리 후 로그인 서버용",
		description = AuthApiDocs.APPLE_FORM_POST_CALLBACK
	)
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.AUTHENTICATION_FAILED
	})
	@PostMapping(value = "/apple", consumes = "application/x-www-form-urlencoded")
	public ApiResponse<SocialLoginData> callback(
		@RequestParam("code") String code,
		@RequestParam(value = "user", required = false) String userJson,
		HttpServletResponse response
	) {
		SocialLoginUseCase.LoginResult result = socialLoginUseCase.loginApple(code, userJson);
		tokenHeaderWriter.write(response, result.tokens());
		return ApiResponses.success(SuccessCode.OK, result.data());
	}
}
