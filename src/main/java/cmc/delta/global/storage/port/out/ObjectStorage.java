package cmc.delta.global.storage.port.out;

import java.time.Duration;

public interface ObjectStorage {

	void put(String storageKey, byte[] bytes, String contentType);

	String createPresignedGetUrl(String storageKey, Duration ttl);

	void delete(String storageKey);
	byte[] readBytes(String storageKey);
}
