package cmc.delta.global.storage.support;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class PresignedUrlCache {

	private static final int MAX_ENTRIES = 10_000;
	private static final long MAX_AGE_MS = 30_000; // keep short; URL TTL can be 10min+

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
		long expiresAtMs = System.currentTimeMillis() + Math.min(MAX_AGE_MS, (long)ttlSeconds * 1000L);
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
