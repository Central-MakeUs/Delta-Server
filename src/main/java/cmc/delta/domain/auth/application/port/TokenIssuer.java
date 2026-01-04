package cmc.delta.domain.auth.application.port;

import cmc.delta.global.config.security.principal.UserPrincipal;

public interface TokenIssuer {

	IssuedTokens issue(UserPrincipal principal);

	record IssuedTokens(String accessToken, String refreshToken, String tokenType) {

		public String authorizationHeaderValue() {
			return tokenType + " " + accessToken;
		}
	}
}
