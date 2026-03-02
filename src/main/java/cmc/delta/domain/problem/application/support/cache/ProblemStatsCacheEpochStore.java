package cmc.delta.domain.problem.application.support.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProblemStatsCacheEpochStore extends UserCacheEpochStore {

	private static final String KEY_PREFIX = "cache:problemStats:epoch:";

	public ProblemStatsCacheEpochStore(StringRedisTemplate redis) {
		super(redis, KEY_PREFIX);
	}
}
