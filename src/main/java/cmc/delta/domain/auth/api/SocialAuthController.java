package cmc.delta.domain.auth.api;

import cmc.delta.domain.auth.api.dto.response.SocialLoginData;
import cmc.delta.domain.auth.api.dto.request.SocialLoginRequest;
import cmc.delta.domain.auth.application.AuthHeaderConstants;
import cmc.delta.domain.auth.application.SocialAuthFacade;
import cmc.delta.domain.auth.application.port.TokenIssuer;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/** 카카오 인가코드로 로그인하고, 발급 토큰을 응답 헤더로 내려주는 컨트롤러. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class SocialAuthController {

	private final SocialAuthFacade kakaoAuthService;

	@PostMapping("/kakao")
	public SocialLoginData login(
		@Valid @RequestBody
		SocialLoginRequest request, HttpServletResponse response) {
		SocialAuthFacade.LoginResult result = kakaoAuthService.loginWithCode(request.code());

		TokenIssuer.IssuedTokens tokens = result.tokens();
		response.setHeader(HttpHeaders.AUTHORIZATION, tokens.authorizationHeaderValue());

		if (tokens.refreshToken() != null && !tokens.refreshToken().isBlank()) {
			response.setHeader(AuthHeaderConstants.REFRESH_TOKEN_HEADER, tokens.refreshToken());
		}

		response.setHeader(
			HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, AuthHeaderConstants.EXPOSE_HEADERS_VALUE);

		return result.data();
	}
}
