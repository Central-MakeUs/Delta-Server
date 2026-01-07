package cmc.delta.global.storage.support;

import cmc.delta.global.storage.ObjectStorage;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeObjectStorage implements ObjectStorage {

	private final Map<String, byte[]> store = new ConcurrentHashMap<>();

	@Override
	public void put(String storageKey, byte[] bytes, String contentType) {
		store.put(storageKey, bytes);
	}

	@Override
	public String createPresignedGetUrl(String storageKey, Duration ttl) {
		return "https://fake-s3.local/" + storageKey + "?ttl=" + ttl.toSeconds();
	}

	@Override
	public void delete(String storageKey) {
		store.remove(storageKey);
	}

	public boolean exists(String storageKey) {
		return store.containsKey(storageKey);
	}
}
