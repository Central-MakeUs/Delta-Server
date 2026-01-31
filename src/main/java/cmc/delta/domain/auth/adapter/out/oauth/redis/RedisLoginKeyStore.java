package cmc.delta.domain.auth.adapter.out.oauth.redis;

import cmc.delta.domain.auth.application.port.in.social.SocialLoginData;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisLoginKeyStore {

	private static final String KEY_PREFIX = "lk:";
	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public RedisLoginKeyStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public void save(String loginKey, SocialLoginData data, TokenIssuer.IssuedTokens tokens, Duration ttl) {
		try {
			LoginKeyPayload payload = new LoginKeyPayload(data, tokens);
			String json = objectMapper.writeValueAsString(payload);
			redis.opsForValue().set(KEY_PREFIX + loginKey, json, ttl);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Stored consume(String loginKey) {
		String key = KEY_PREFIX + loginKey;
		String json = redis.opsForValue().get(key);
		if (json == null)
			return null;
		redis.delete(key);
		try {
			LoginKeyPayload payload = objectMapper.readValue(json, LoginKeyPayload.class);
			SocialLoginData data = payload.data();
			TokenIssuer.IssuedTokens tokens = payload.tokens();
			return new Stored(data, tokens);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static record Stored(SocialLoginData data, TokenIssuer.IssuedTokens tokens) {
	}
}
