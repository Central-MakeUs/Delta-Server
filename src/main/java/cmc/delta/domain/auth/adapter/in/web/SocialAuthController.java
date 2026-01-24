package cmc.delta.domain.auth.adapter.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cmc.delta.domain.auth.adapter.in.support.AuthHeaderConstants;
import cmc.delta.domain.auth.adapter.in.web.dto.request.KakaoLoginRequest;
import cmc.delta.domain.auth.adapter.in.web.dto.response.SocialLoginData;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.auth.application.service.social.SocialAuthFacade;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
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

	private final SocialAuthFacade socialAuthFacade;

	@Operation(summary = "카카오 인가코드로 로그인")
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.AUTHENTICATION_FAILED
	})
	@PostMapping("/kakao")
	public ApiResponse<SocialLoginData> kakao(
		@Valid @RequestBody KakaoLoginRequest request,
		HttpServletResponse response
	) {
		SocialAuthFacade.LoginResult result = socialAuthFacade.loginKakao(request.code());
		setTokenHeaders(response, result.tokens());
		return ApiResponses.success(SuccessCode.OK, result.data());
	}

	@Operation(summary = "애플 콜백(form_post) 처리 후 로그인")
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
		SocialAuthFacade.LoginResult result = socialAuthFacade.loginApple(code, userJson);
		setTokenHeaders(response, result.tokens());
		return ApiResponses.success(SuccessCode.OK, result.data());
	}

	private void setTokenHeaders(HttpServletResponse response, TokenIssuer.IssuedTokens tokens) {
		response.setHeader(HttpHeaders.AUTHORIZATION, tokens.authorizationHeaderValue());

		if (tokens.refreshToken() != null && !tokens.refreshToken().isBlank()) {
			response.setHeader(AuthHeaderConstants.REFRESH_TOKEN_HEADER, tokens.refreshToken());
		}

		response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, AuthHeaderConstants.EXPOSE_HEADERS_VALUE);
	}
}
