package cmc.delta.domain.auth.infrastructure.oauth.token.redis;

import cmc.delta.domain.auth.application.port.RefreshTokenStore;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** Redis에 Refresh 토큰 해시를 TTL로 저장하고 rotate를 원자 처리한다. */
@Component
public class RedisRefreshTokenStore implements RefreshTokenStore {

	private static final String KEY_PREFIX = "rt:";

	private final StringRedisTemplate redis;
	private final DefaultRedisScript<Long> rotateScript;

	public RedisRefreshTokenStore(StringRedisTemplate redis) {
		this.redis = redis;
		this.rotateScript = buildRotateScript();
	}

	@Override
	public void refreshSave(Long userId, String sessionId, String refreshTokenHash, Duration ttl) {
		// Refresh 토큰 해시를 TTL로 저장한다.
		String key = key(userId, sessionId);
		if (invalid(key, refreshTokenHash, ttl))
			return;

		redis.opsForValue().set(key, refreshTokenHash, ttl);
	}

	@Override
	public RotationResult refreshRotate(
		Long userId, String sessionId, String expectedHash, String newHash, Duration ttl) {
		// 기존 해시와 일치할 때만 새 해시로 교체한다.
		String key = key(userId, sessionId);
		if (invalid(key, expectedHash, ttl) || newHash == null || newHash.isBlank())
			return RotationResult.MISMATCH;

		Long result = redis.execute(
			rotateScript, List.of(key), expectedHash, newHash, String.valueOf(ttl.toMillis()));

		if (result == null)
			return RotationResult.MISMATCH;
		if (result == 1L)
			return RotationResult.ROTATED;
		if (result == 0L)
			return RotationResult.NOT_FOUND;
		return RotationResult.MISMATCH;
	}

	@Override
	public void refreshDelete(Long userId, String sessionId) {
		// 해당 세션의 Refresh 토큰을 삭제한다.
		redis.delete(key(userId, sessionId));
	}

	private String key(Long userId, String sessionId) {
		// userId/sessionId로 Redis 키를 만든다.
		String sid = (sessionId == null || sessionId.isBlank()) ? "default" : sessionId;
		return KEY_PREFIX + userId + ":" + sid;
	}

	private boolean invalid(String key, String hash, Duration ttl) {
		// 필수 값 검증을 단순화한다.
		if (key == null || key.isBlank())
			return true;
		if (hash == null || hash.isBlank())
			return true;
		return ttl == null || ttl.isZero() || ttl.isNegative();
	}

	private DefaultRedisScript<Long> buildRotateScript() {
		// GET -> 비교 -> PSETEX 를 원자적으로 수행한다.
		String lua = """
			local cur = redis.call('GET', KEYS[1])
			if not cur then
			  return 0
			end
			if cur ~= ARGV[1] then
			  return -1
			end
			redis.call('PSETEX', KEYS[1], ARGV[3], ARGV[2])
			return 1
			""";

		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setResultType(Long.class);
		script.setScriptText(lua);
		return script;
	}
}
