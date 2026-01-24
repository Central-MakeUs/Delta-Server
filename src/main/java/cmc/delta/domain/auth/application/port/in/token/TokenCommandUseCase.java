package cmc.delta.domain.auth.application.port.in.token;

import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.global.config.security.principal.UserPrincipal;

public interface TokenCommandUseCase {
	TokenIssuer.IssuedTokens issue(UserPrincipal principal);

	TokenIssuer.IssuedTokens reissue(String refreshToken);

	void logout(long userId, String accessToken, String refreshToken);

	void invalidateAll(long userId, String accessTokenOrNull);
}
