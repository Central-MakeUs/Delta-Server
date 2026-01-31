package cmc.delta.domain.auth.application.port.in.loginkey;

import cmc.delta.domain.auth.adapter.out.oauth.redis.RedisLoginKeyStore;
import cmc.delta.domain.auth.application.port.in.social.SocialLoginData;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import java.time.Duration;

/**
 * 포트: loginKey 저장 및 교환(1회성) 기능을 제공하는 UseCase
 */
public interface LoginKeyExchangeUseCase {

	/**
	 * Save loginKey payload with TTL.
	 */
	void save(String loginKey, SocialLoginData data, TokenIssuer.IssuedTokens tokens, Duration ttl);

	/**
	 * Consume loginKey and return stored payload or null when missing.
	 */
	RedisLoginKeyStore.Stored exchange(String loginKey);
}
