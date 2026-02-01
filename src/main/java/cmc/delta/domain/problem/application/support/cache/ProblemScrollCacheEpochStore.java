package cmc.delta.domain.problem.application.support.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ProblemScrollCacheEpochStore {

	private static final String KEY_PREFIX = "cache:wrongAnswerPages:epoch:";

	private final StringRedisTemplate redis;

	public ProblemScrollCacheEpochStore(StringRedisTemplate redis) {
		this.redis = redis;
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
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			bumpNow(userId);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				bumpNow(userId);
			}
		});
	}

	private void bumpNow(Long userId) {
		redis.opsForValue().increment(key(userId));
	}

	private String key(Long userId) {
		return KEY_PREFIX + userId;
	}
}
