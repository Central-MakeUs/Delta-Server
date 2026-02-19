package cmc.delta.domain.auth.adapter.out.oauth.loginkey;

import cmc.delta.domain.auth.application.port.in.social.SocialLoginData;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis에 loginKey와 연관된 페이로드를 저장하고 1회성으로 소비하는 책임을 가진 컴포넌트입니다.
 */
@Component
public class RedisLoginKeyStore {

	private static final String KEY_PREFIX = "lk:";
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public RedisLoginKeyStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	/**
	 * loginKey와 연관된 사용자 데이터 및 토큰을 Redis에 저장합니다.
	 * 저장 시 TTL을 적용하여 단기간만 유효하도록 보장합니다.
	 */
	public void save(String loginKey, SocialLoginData data, TokenIssuer.IssuedTokens tokens, Duration ttl) {
		try {
			LoginKeyPayload payload = new LoginKeyPayload(data, tokens);
			String json = objectMapper.writeValueAsString(payload);
			redisTemplate.opsForValue().set(KEY_PREFIX + loginKey, json, ttl);
		} catch (Exception e) {
			throw LoginKeyStoreException.saveFailed(e);
		}
	}

	/**
	 * 주어진 loginKey에 해당하는 값을 읽고 즉시 삭제합니다. 키가 없으면 null을 반환합니다.
	 */
	public Stored consume(String loginKey) {
		String key = KEY_PREFIX + loginKey;
		// 두 소비자가 동일한 키를 동시에 읽는 race 조건을 피하기 위해
		// Redis의 getAndDelete를 사용하여 원자적으로 읽고 삭제합니다.
		String json = redisTemplate.opsForValue().getAndDelete(key);
		if (json == null) {
			return null;
		}
		try {
			LoginKeyPayload payload = objectMapper.readValue(json, LoginKeyPayload.class);
			return new Stored(payload.data(), payload.tokens());
		} catch (Exception e) {
			throw LoginKeyStoreException.readFailed(e);
		}
	}

	public static record Stored(SocialLoginData data, TokenIssuer.IssuedTokens tokens) {
	}
}
