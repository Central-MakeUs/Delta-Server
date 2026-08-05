package cmc.delta.global.storage.support;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class PresignedUrlCache {

	private static final int MAX_ENTRIES = 10_000;
	private static final long EXPIRY_BUFFER_MS = 60_000; // URL 만료 60초 전에 캐시 선제 무효화

	private final ConcurrentHashMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

	public Optional<String> get(String storageKey, int ttlSeconds) {
		CacheEntry entry = cache.get(new CacheKey(storageKey, ttlSeconds));
		if (entry == null || entry.isExpired()) {
			return Optional.empty();
		}
		return Optional.of(entry.url());
	}

	public void put(String storageKey, int ttlSeconds, String url) {
		evictIfNeeded();
		long urlLifetimeMs = TimeUnit.SECONDS.toMillis(ttlSeconds);
		long cacheLifetimeMs = Math.max(0, urlLifetimeMs - EXPIRY_BUFFER_MS);
		long expiresAtMs = System.currentTimeMillis() + cacheLifetimeMs;
		cache.put(new CacheKey(storageKey, ttlSeconds), new CacheEntry(url, expiresAtMs));
	}

	/** 만료 항목부터 제거하고, 그래도 한도를 넘으면 전체 초기화한다(핫 엔트리 전면 폐기는 최후 수단). */
	private void evictIfNeeded() {
		if (cache.size() <= MAX_ENTRIES) {
			return;
		}
		cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
		if (cache.size() > MAX_ENTRIES) {
			cache.clear();
		}
	}

	private record CacheKey(String storageKey, int ttlSeconds) {
	}

	private record CacheEntry(String url, long expiresAtMs) {

		boolean isExpired() {
			return expiresAtMs <= System.currentTimeMillis();
		}
	}
}
