package cmc.delta.domain.auth.application.port.in.loginkey;

import cmc.delta.domain.auth.adapter.out.oauth.loginkey.RedisLoginKeyStore;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginData;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import java.time.Duration;

/**
 * 포트: loginKey 저장 및 교환(1회성) 기능을 제공하는 UseCase
 */
public interface LoginKeyExchangeUseCase {

	void save(String loginKey, SocialLoginData data, TokenIssuer.IssuedTokens tokens, Duration ttl);

	RedisLoginKeyStore.Stored exchange(String loginKey);
}
