package cmc.delta.domain.auth.application.support;

import cmc.delta.domain.auth.application.port.AccessBlacklistStore;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAccessBlacklistStore implements AccessBlacklistStore {

	private final Map<String, Duration> blacklisted = new ConcurrentHashMap<>();

	@Override
	public boolean isBlacklisted(String jti) {
		return jti != null && !jti.isBlank() && blacklisted.containsKey(jti);
	}

	@Override
	public void blacklist(String jti, Duration ttl) {
		if (jti == null || jti.isBlank())
			return;
		if (ttl == null || ttl.isZero() || ttl.isNegative())
			return;

		blacklisted.put(jti, ttl);
	}

	public Duration getLastTtlOrNull(String jti) {
		return blacklisted.get(jti);
	}
}
