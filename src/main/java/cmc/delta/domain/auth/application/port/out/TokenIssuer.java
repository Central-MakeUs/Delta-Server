package cmc.delta.domain.auth.application.port.out;

import cmc.delta.global.config.security.principal.UserPrincipal;
import java.time.Duration;

public interface TokenIssuer {

	IssuedTokens issue(UserPrincipal principal);

	UserPrincipal extractPrincipalFromRefreshToken(String refreshToken);

	AccessTokenInfo parseAccessTokenInfo(String accessToken);

	record AccessTokenInfo(String jti, Duration remainingTtl) {}


	record IssuedTokens(String accessToken, String refreshToken, String tokenType) {
		public String authorizationHeaderValue() {
			return tokenType + " " + accessToken;
		}
	}
}
