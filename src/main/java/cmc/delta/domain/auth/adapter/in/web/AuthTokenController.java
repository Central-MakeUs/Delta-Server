package cmc.delta.domain.auth.adapter.in.web;

import cmc.delta.domain.auth.adapter.in.support.AuthHeaderConstants;
import cmc.delta.domain.auth.adapter.in.support.HttpTokenExtractor;
import cmc.delta.domain.auth.application.port.in.token.ReissueTokenUseCase;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.config.security.principal.CurrentUser;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
import cmc.delta.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthTokenController {

	private final ReissueTokenUseCase tokenUseCase;
	private final HttpTokenExtractor httpTokenExtractor;

	@Operation(summary = "토큰 재발급")
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.AUTHENTICATION_FAILED
	})
	@PostMapping("/reissue")
	public ApiResponse<Void> reissue(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = httpTokenExtractor.extractRefreshToken(request);
		TokenIssuer.IssuedTokens tokens = tokenUseCase.reissue(refreshToken);

		response.setHeader(HttpHeaders.AUTHORIZATION, tokens.authorizationHeaderValue());
		response.setHeader(AuthHeaderConstants.REFRESH_TOKEN_HEADER, tokens.refreshToken());
		response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, AuthHeaderConstants.EXPOSE_HEADERS_VALUE);

		return ApiResponses.success(200);
	}

	@Operation(summary = "로그아웃")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.ACCESS_DENIED
	})
	@PostMapping("/logout")
	public ApiResponse<Void> logout(@CurrentUser UserPrincipal principal, HttpServletRequest request) {
		String accessToken = httpTokenExtractor.extractAccessToken(request);
		tokenUseCase.invalidateAll(principal.userId(), accessToken);

		return ApiResponses.success(200);
	}
}
