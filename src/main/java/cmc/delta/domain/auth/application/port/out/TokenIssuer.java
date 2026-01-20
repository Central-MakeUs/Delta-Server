package cmc.delta.domain.auth.application.port.out;

import cmc.delta.global.config.security.principal.UserPrincipal;
import java.time.Duration;

public interface TokenIssuer {

	IssuedTokens issue(UserPrincipal principal);

	Long extractUserIdFromRefreshToken(String refreshToken);

	String extractJtiFromAccessToken(String accessToken);

	Duration remainingAccessTtl(String accessToken);

	record IssuedTokens(String accessToken, String refreshToken, String tokenType) {
		public String authorizationHeaderValue() {
			return tokenType + " " + accessToken;
		}
	}
}
