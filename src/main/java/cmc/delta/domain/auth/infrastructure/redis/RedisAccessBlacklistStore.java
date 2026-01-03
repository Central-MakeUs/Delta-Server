package cmc.delta.domain.auth.infrastructure.redis;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import cmc.delta.domain.auth.application.port.AccessBlacklistStore;

/** Redis에 access 토큰 jti를 TTL로 저장한다. */
@Component
@ConditionalOnProperty(prefix = "jwt.blacklist", name = "enabled", havingValue = "true")
public class RedisAccessBlacklistStore implements AccessBlacklistStore {

    private static final String KEY_PREFIX = "bl:access:";

    private final StringRedisTemplate redis;

    public RedisAccessBlacklistStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean isBlacklisted(String jti) {
        // jti 키가 존재하면 블랙리스트로 판단한다.
        if (jti == null || jti.isBlank()) return false;
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + jti));
    }

    @Override
    public void blacklist(String jti, Duration ttl) {
        // 남은 만료 시간만큼만 블랙리스트를 유지한다.
        if (jti == null || jti.isBlank()) return;
        if (ttl == null || ttl.isZero() || ttl.isNegative()) return;
        redis.opsForValue().set(KEY_PREFIX + jti, "1", ttl);
    }
}
