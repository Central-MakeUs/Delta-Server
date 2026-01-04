package cmc.delta.domain.auth.application.support;

import cmc.delta.domain.auth.application.port.RefreshTokenStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRefreshTokenStore implements RefreshTokenStore {

	private final Clock clock;
	private final Map<String, Entry> store = new ConcurrentHashMap<>();

	public InMemoryRefreshTokenStore(Clock clock) {
		this.clock = clock;
	}

	@Override
	public void refreshSave(Long userId, String sessionId, String refreshTokenHash, Duration ttl) {
		if (userId == null)
			return;
		if (isBlank(refreshTokenHash))
			return;
		if (invalidTtl(ttl))
			return;

		store.put(key(userId, sessionId), new Entry(refreshTokenHash, expiresAt(ttl)));
	}

	@Override
	public RotationResult refreshRotate(
		Long userId, String sessionId, String expectedHash, String newHash, Duration ttl) {

		if (userId == null)
			return RotationResult.MISMATCH;
		if (isBlank(expectedHash) || isBlank(newHash))
			return RotationResult.MISMATCH;
		if (invalidTtl(ttl))
			return RotationResult.MISMATCH;

		String key = key(userId, sessionId);
		Entry current = store.get(key);

		if (current == null || current.isExpired(clock.instant())) {
			store.remove(key);
			return RotationResult.NOT_FOUND;
		}

		if (!current.hash.equals(expectedHash)) {
			return RotationResult.MISMATCH;
		}

		store.put(key, new Entry(newHash, expiresAt(ttl)));
		return RotationResult.ROTATED;
	}

	@Override
	public void refreshDelete(Long userId, String sessionId) {
		if (userId == null)
			return;
		store.remove(key(userId, sessionId));
	}

	public String getSavedHashOrNull(long userId, String sessionId) {
		Entry entry = store.get(key(userId, sessionId));
		if (entry == null)
			return null;

		if (entry.isExpired(clock.instant())) {
			store.remove(key(userId, sessionId));
			return null;
		}
		return entry.hash;
	}

	private String key(Long userId, String sessionId) {
		String sid = (sessionId == null || sessionId.isBlank()) ? "default" : sessionId;
		return "rt:" + userId + ":" + sid;
	}

	private Instant expiresAt(Duration ttl) {
		return clock.instant().plus(ttl);
	}

	private boolean invalidTtl(Duration ttl) {
		return ttl == null || ttl.isZero() || ttl.isNegative();
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record Entry(String hash, Instant expiresAt) {
		boolean isExpired(Instant now) {
			return expiresAt != null && !expiresAt.isAfter(now);
		}
	}
}
