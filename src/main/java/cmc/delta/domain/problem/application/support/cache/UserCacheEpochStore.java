package cmc.delta.domain.problem.application.support.cache;

import cmc.delta.global.transaction.TransactionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

public abstract class UserCacheEpochStore {

	private final StringRedisTemplate redis;
	private final String keyPrefix;

	protected UserCacheEpochStore(StringRedisTemplate redis, String keyPrefix) {
		this.redis = redis;
		this.keyPrefix = keyPrefix;
	}

	public long getEpoch(Long userId) {
		if (userId == null) {
			return 0L;
		}
		String raw = redis.opsForValue().get(key(userId));
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(raw);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	public void bumpAfterCommit(Long userId) {
		if (userId == null) {
			return;
		}
		TransactionUtils.afterCommit(() -> bumpNow(userId));
	}

	private void bumpNow(Long userId) {
		redis.opsForValue().increment(key(userId));
	}

	private String key(Long userId) {
		return keyPrefix + userId;
	}
}
