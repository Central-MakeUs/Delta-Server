package cmc.delta.domain.auth.api;

import cmc.delta.domain.auth.application.port.TokenIssuer;
import cmc.delta.domain.auth.application.token.AuthHeaderConstants;
import cmc.delta.domain.auth.application.token.TokenService;
import cmc.delta.domain.auth.application.token.dto.ActionResultData;
import cmc.delta.domain.auth.application.token.validator.TokenValidator;
import cmc.delta.global.config.security.principal.CurrentUser;
import cmc.delta.global.config.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthTokenController {

	private final TokenService tokenService;
	private final TokenValidator tokenValidator;

	@PostMapping("/reissue")
	public ActionResultData reissue(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = tokenValidator.extractRefreshToken(request);

		TokenIssuer.IssuedTokens tokens = tokenService.reissue(refreshToken);

		response.setHeader(HttpHeaders.AUTHORIZATION, tokens.authorizationHeaderValue());
		response.setHeader(AuthHeaderConstants.REFRESH_TOKEN_HEADER, tokens.refreshToken());
		response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, AuthHeaderConstants.EXPOSE_HEADERS_VALUE);

		return ActionResultData.success();
	}

	@PostMapping("/logout")
	public ActionResultData logout(@CurrentUser UserPrincipal principal, HttpServletRequest request) {
		String accessToken = tokenValidator.extractAccessToken(request);
		String refreshToken = tokenValidator.extractRefreshToken(request);

		tokenService.logout(principal.userId(), accessToken, refreshToken);
		return ActionResultData.success();
	}
}
