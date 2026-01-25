package cmc.delta.domain.auth.adapter.in.web;

import cmc.delta.domain.auth.adapter.in.support.HttpTokenExtractor;
import cmc.delta.domain.auth.adapter.in.support.TokenHeaderWriter;
import cmc.delta.domain.auth.application.port.in.token.TokenCommandUseCase;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthTokenController {

	private final TokenCommandUseCase tokenCommandUseCase;
	private final HttpTokenExtractor httpTokenExtractor;
	private final TokenHeaderWriter tokenHeaderWriter;

	@Operation(summary = "토큰 재발급")
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.AUTHENTICATION_FAILED
	})
	@PostMapping("/reissue")
	public ApiResponse<Void> reissue(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = httpTokenExtractor.extractRefreshToken(request);
		TokenIssuer.IssuedTokens tokens = tokenCommandUseCase.reissue(refreshToken);
		tokenHeaderWriter.write(response, tokens);

		return ApiResponses.success(200);
	}

	@Operation(summary = "로그아웃")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.ACCESS_DENIED
	})
	@PostMapping("/logout")
	public ApiResponse<Void> logout(@CurrentUser
	UserPrincipal principal, HttpServletRequest request) {
		String accessToken = httpTokenExtractor.extractAccessToken(request);
		tokenCommandUseCase.invalidateAll(principal.userId(), accessToken);

		return ApiResponses.success(200);
	}
}
