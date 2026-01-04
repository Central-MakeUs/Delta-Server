package cmc.delta.domain.auth.application.token;

import cmc.delta.domain.auth.application.port.TokenIssuer;
import cmc.delta.global.config.security.principal.UserPrincipal;

public interface TokenService {

	TokenIssuer.IssuedTokens issue(UserPrincipal principal);

	TokenIssuer.IssuedTokens reissue(String refreshToken);

	void logout(long userId, String accessToken, String refreshToken);

	void invalidateAll(long userId, String accessTokenOrNull);
}
