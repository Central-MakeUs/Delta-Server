package cmc.delta.global.storage;

import java.time.Duration;

public interface ObjectStorage {

	StoredObject upload(UploadRequest request);

	String generateReadUrl(String storageKey, Duration ttl);

	void deleteObject(String storageKey);

	record UploadRequest(
		String directory,
		String originalFilename,
		String contentType,
		byte[] bytes
	) {}

	record StoredObject(
		String storageKey,
		String contentType,
		long sizeBytes,
		Integer width,
		Integer height
	) {}
}
