package cmc.delta.global.storage.support;

import cmc.delta.global.storage.port.out.ObjectStorage;
import cmc.delta.global.storage.port.out.StoredObjectStream;
import java.io.ByteArrayInputStream;
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
	public byte[] readBytes(String storageKey) {
		byte[] bytes = store.get(storageKey);
		if (bytes == null) {
			throw new IllegalStateException("OBJECT_NOT_FOUND");
		}
		return bytes;
	}

	@Override
	public String createPresignedGetUrl(String storageKey, Duration ttl) {
		return "https://fake-s3.local/" + storageKey + "?ttl=" + ttl.toSeconds();
	}

	@Override
	public void delete(String storageKey) {
		store.remove(storageKey);
	}

	@Override
	public void copy(String sourceStorageKey, String destinationStorageKey) {
		byte[] bytes = store.get(sourceStorageKey);
		if (bytes == null) {
			throw new IllegalStateException("OBJECT_NOT_FOUND");
		}
		store.put(destinationStorageKey, bytes);
	}

	@Override
	public StoredObjectStream openStream(String storageKey) {
		byte[] bytes = readBytes(storageKey);
		return new StoredObjectStream(new ByteArrayInputStream(bytes), bytes.length);
	}

	public boolean exists(String storageKey) {
		return store.containsKey(storageKey);
	}
}
