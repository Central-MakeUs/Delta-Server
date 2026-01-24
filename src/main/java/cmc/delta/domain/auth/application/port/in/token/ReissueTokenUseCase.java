package cmc.delta.domain.auth.application.port.in.token;

import cmc.delta.domain.auth.application.port.out.TokenIssuer;

public interface ReissueTokenUseCase {

	TokenIssuer.IssuedTokens reissue(String refreshToken);

	void invalidateAll(long userId, String accessTokenOrNull);
}
