package cmc.delta.global.api.storage;

import cmc.delta.global.storage.ObjectStorage;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/storage")
public class StorageController {

	private static final String DEFAULT_DIRECTORY = "temp";

	private final ObjectStorage objectStorage;

	@PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UploadImageData uploadImage(
		@RequestPart("file") MultipartFile file,
		@RequestParam(value = "directory", required = false) String directory
	) throws Exception {

		ObjectStorage.StoredObject stored = objectStorage.upload(new ObjectStorage.UploadRequest(
			(directory == null || directory.isBlank()) ? DEFAULT_DIRECTORY : directory,
			file.getOriginalFilename(),
			file.getContentType(),
			file.getBytes()
		));

		String viewUrl = objectStorage.generateReadUrl(stored.storageKey(), null);

		return new UploadImageData(
			stored.storageKey(),
			viewUrl,
			stored.contentType(),
			stored.sizeBytes(),
			stored.width(),
			stored.height()
		);
	}

	@GetMapping("/images/presigned-get")
	public PresignedGetData presignGet(
		@RequestParam("key") @NotBlank String storageKey,
		@RequestParam(value = "ttlSeconds", required = false) Integer ttlSeconds
	) {
		Duration ttl = (ttlSeconds == null) ? null : Duration.ofSeconds(ttlSeconds);
		String url = objectStorage.generateReadUrl(storageKey, ttl);
		int expiresInSeconds = (ttlSeconds == null) ? 0 : ttlSeconds;
		return new PresignedGetData(url, expiresInSeconds);
	}

	@DeleteMapping("/images")
	public Map<String, Object> deleteImage(@RequestParam("key") @NotBlank String storageKey) {
		objectStorage.deleteObject(storageKey);
		return Map.of(); // ApiResponseAdvice가 감싸서 data={}
	}

	public record UploadImageData(
		String storageKey,
		String viewUrl,
		String contentType,
		long sizeBytes,
		Integer width,
		Integer height
	) {}

	public record PresignedGetData(
		String url,
		int expiresInSeconds
	) {}
}
