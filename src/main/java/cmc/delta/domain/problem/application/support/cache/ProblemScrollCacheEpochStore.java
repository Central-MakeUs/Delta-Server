package cmc.delta.domain.problem.application.support.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProblemScrollCacheEpochStore extends UserCacheEpochStore {

	private static final String KEY_PREFIX = "cache:wrongAnswerPages:epoch:";

	public ProblemScrollCacheEpochStore(StringRedisTemplate redis) {
		super(redis, KEY_PREFIX);
	}
}
