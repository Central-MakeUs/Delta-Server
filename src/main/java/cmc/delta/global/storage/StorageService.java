package cmc.delta.global.storage;

import cmc.delta.global.storage.s3.S3Properties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StorageService {

	private static final String DEFAULT_DIRECTORY = "temp";
	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
	private static final String PATH_SEPARATOR = "/";
	private static final String DOT = ".";

	private final ObjectStorage objectStorage;
	private final S3Properties properties;

	public UploadImageResult uploadImage(MultipartFile file, String directory) {
		validateFile(file);

		String resolvedDirectory = resolveDirectory(directory);
		String contentType = resolveContentType(file.getContentType());
		byte[] bytes = readBytes(file);

		ImageSize imageSize = tryReadImageSize(bytes);
		String storageKey = createStorageKey(resolvedDirectory, file.getOriginalFilename());

		objectStorage.put(storageKey, bytes, contentType);

		Duration ttl = Duration.ofSeconds(properties.presignGetTtlSeconds());
		String viewUrl = objectStorage.createPresignedGetUrl(storageKey, ttl);

		return new UploadImageResult(
			storageKey,
			viewUrl,
			contentType,
			bytes.length,
			imageSize.width(),
			imageSize.height()
		);
	}

	public PresignedGetUrlResult issueReadUrl(String storageKey, Integer ttlSecondsOrNull) {
		if (!StringUtils.hasText(storageKey)) {
			throw StorageException.invalidRequest("storageKey가 비어있습니다.");
		}

		int ttlSeconds = (ttlSecondsOrNull == null)
			? properties.presignGetTtlSeconds()
			: ttlSecondsOrNull;

		if (ttlSeconds < 60) {
			throw StorageException.invalidRequest("ttlSeconds는 60 이상이어야 합니다.");
		}

		Duration ttl = Duration.ofSeconds(ttlSeconds);
		String url = objectStorage.createPresignedGetUrl(storageKey, ttl);
		return new PresignedGetUrlResult(url, ttlSeconds);
	}

	public void deleteImage(String storageKey) {
		if (!StringUtils.hasText(storageKey)) {
			throw StorageException.invalidRequest("storageKey가 비어있습니다.");
		}
		objectStorage.delete(storageKey);
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw StorageException.invalidRequest("파일이 비어있습니다.");
		}
		if (file.getSize() > properties.maxUploadBytes()) {
			throw StorageException.invalidRequest("파일 용량이 너무 큽니다.");
		}
	}

	private byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (Exception e) {
			throw StorageException.internalError("파일을 읽는 중 오류가 발생했습니다.", e);
		}
	}

	private String resolveDirectory(String directory) {
		return StringUtils.hasText(directory) ? directory : DEFAULT_DIRECTORY;
	}

	private String resolveContentType(String contentType) {
		return StringUtils.hasText(contentType) ? contentType : DEFAULT_CONTENT_TYPE;
	}

	private String createStorageKey(String directory, String originalFilename) {
		String prefix = normalizeDirectory(properties.keyPrefix());
		String dir = normalizeDirectory(directory);

		String extension = extractExtension(originalFilename);
		String randomName = UUID.randomUUID().toString().replace("-", "");
		String filename = (extension == null) ? randomName : randomName + DOT + extension;

		return prefix + PATH_SEPARATOR + dir + PATH_SEPARATOR + filename;
	}

	private String normalizeDirectory(String directory) {
		if (!StringUtils.hasText(directory)) {
			throw StorageException.invalidRequest("directory가 비어있습니다.");
		}
		String d = directory.trim();
		while (d.startsWith(PATH_SEPARATOR)) d = d.substring(1);
		while (d.endsWith(PATH_SEPARATOR)) d = d.substring(0, d.length() - 1);

		if (!StringUtils.hasText(d)) {
			throw StorageException.invalidRequest("directory가 비어있습니다.");
		}
		return d;
	}

	private String extractExtension(String originalFilename) {
		if (!StringUtils.hasText(originalFilename)) {
			return null;
		}
		int idx = originalFilename.lastIndexOf(DOT);
		if (idx < 0 || idx == originalFilename.length() - 1) {
			return null;
		}
		return originalFilename.substring(idx + 1).toLowerCase(Locale.ROOT);
	}

	private ImageSize tryReadImageSize(byte[] bytes) {
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			if (image == null) return ImageSize.empty();
			return new ImageSize(image.getWidth(), image.getHeight());
		} catch (Exception ignored) {
			return ImageSize.empty();
		}
	}

	public record UploadImageResult(
		String storageKey,
		String viewUrl,
		String contentType,
		long sizeBytes,
		Integer width,
		Integer height
	) {}

	public record PresignedGetUrlResult(
		String url,
		int expiresInSeconds
	) {}

	private record ImageSize(Integer width, Integer height) {
		static ImageSize empty() {
			return new ImageSize(null, null);
		}
	}
}
