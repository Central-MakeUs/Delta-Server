package cmc.delta.global.storage.port.out;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface StoragePort {
	String issueReadUrl(String storageKey);

	Map<String, String> issueReadUrls(List<String> storageKeys);

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
