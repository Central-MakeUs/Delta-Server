package cmc.delta.domain.auth.application.port.in.social;

import cmc.delta.domain.auth.application.port.out.TokenIssuer;

public interface SocialLoginCommandUseCase {
	LoginResult loginKakao(String code);

	LoginResult loginApple(String code, String userJson);

	record LoginResult(SocialLoginData data, TokenIssuer.IssuedTokens tokens) {}
}
