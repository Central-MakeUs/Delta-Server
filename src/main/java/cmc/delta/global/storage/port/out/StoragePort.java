package cmc.delta.global.storage.port.out;

import org.springframework.web.multipart.MultipartFile;

public interface StoragePort {
	String issueReadUrl(String storageKey);

	UploadResult uploadImage(MultipartFile file, String directory);

	void deleteImage(String storageKey);

	record UploadResult(
		String storageKey,
		String viewUrl,
		String contentType,
		long sizeBytes,
		Integer width,
		Integer height) {
	}
}
