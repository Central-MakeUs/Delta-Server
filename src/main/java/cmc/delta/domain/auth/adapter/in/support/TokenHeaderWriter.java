package cmc.delta.domain.auth.adapter.in.support;

import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TokenHeaderWriter {

	public void write(HttpServletResponse response, TokenIssuer.IssuedTokens tokens) {
		response.setHeader(HttpHeaders.AUTHORIZATION, tokens.authorizationHeaderValue());

		if (StringUtils.hasText(tokens.refreshToken())) {
			response.setHeader(AuthHeaderConstants.REFRESH_TOKEN_HEADER, tokens.refreshToken());
		}

		response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, AuthHeaderConstants.EXPOSE_HEADERS_VALUE);
	}
}
