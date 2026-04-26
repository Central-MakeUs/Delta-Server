package cmc.delta.global.storage.support;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class PresignedUrlCache {

	private static final int MAX_ENTRIES = 10_000;
	private static final long EXPIRY_BUFFER_MS = 60_000; // URL 만료 60초 전에 캐시 선제 무효화

	private final ConcurrentHashMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

	public String get(String storageKey, int ttlSeconds) {
		CacheKey key = new CacheKey(storageKey, ttlSeconds);
		CacheEntry entry = cache.get(key);
		if (entry != null && entry.expiresAtMs() > System.currentTimeMillis()) {
			return entry.url();
		}
		return null;
	}

	public void put(String storageKey, int ttlSeconds, String url) {
		evictIfNeeded();
		long urlLifetimeMs = (long) ttlSeconds * 1000L;
		long cacheLifetimeMs = Math.max(0, urlLifetimeMs - EXPIRY_BUFFER_MS);
		long expiresAtMs = System.currentTimeMillis() + cacheLifetimeMs;
		cache.put(new CacheKey(storageKey, ttlSeconds), new CacheEntry(url, expiresAtMs));
	}

	private void evictIfNeeded() {
		if (cache.size() > MAX_ENTRIES) {
			cache.clear();
		}
	}

	private record CacheKey(String storageKey, int ttlSeconds) {
	}

	private record CacheEntry(String url, long expiresAtMs) {
	}
}
