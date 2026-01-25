package cmc.delta.global.storage.application;

import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.api.storage.dto.StorageUploadData;
import cmc.delta.global.storage.adapter.out.s3.S3Properties;
import cmc.delta.global.storage.exception.StorageException;
import cmc.delta.global.storage.port.out.ObjectStorage;
import cmc.delta.global.storage.support.ImageMetadataExtractor;
import cmc.delta.global.storage.support.StorageKeyGenerator;
import cmc.delta.global.storage.support.StorageRequestValidator;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

	private static final String DEFAULT_DIRECTORY = "temp";
	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

	private final ObjectStorage objectStorage;
	private final S3Properties properties;

	private final StorageRequestValidator validator;
	private final StorageKeyGenerator keyGenerator;

	public StorageUploadData uploadImage(MultipartFile file, String directory) {
		long startedAt = System.nanoTime();

		validator.validateUploadFile(file, properties.maxUploadBytes());

		String resolvedDirectory = resolveDirectory(directory);
		String contentType = resolveContentType(file.getContentType());
		if (!isImageContentType(contentType)) {
			throw StorageException.invalidRequest("이미지 파일만 업로드할 수 있습니다.");
		}

		byte[] bytes = readBytes(file);

		// (추가해둔 validator라면) bytes 기반으로도 한 번 더 방어 가능
		validator.validateUploadBytes(bytes, properties.maxUploadBytes());

		ImageMetadataExtractor.ImageSize imageSize = requireValidImage(bytes);

		String storageKey = keyGenerator.generate(resolvedDirectory, file.getOriginalFilename());
		objectStorage.put(storageKey, bytes, contentType);

		String viewUrl = createPresignedGetUrl(storageKey, properties.presignGetTtlSeconds());
		long durationMs = elapsedMs(startedAt);

		logUploadComplete(storageKey, resolvedDirectory, contentType, bytes.length, imageSize, durationMs);

		return new StorageUploadData(
			storageKey,
			viewUrl,
			contentType,
			bytes.length,
			imageSize.width(),
			imageSize.height());
	}

	/**
	 * UseCase가 MultipartFile을 모르도록 하고 싶을 때 쓰는 오버로드.
	 */
	public StorageUploadData uploadImage(byte[] bytes, String contentType, String originalFilename, String directory) {
		long startedAt = System.nanoTime();

		validator.validateUploadBytes(bytes, properties.maxUploadBytes());

		String resolvedDirectory = resolveDirectory(directory);
		String resolvedContentType = resolveContentType(contentType);

		if (!isImageContentType(resolvedContentType)) {
			throw StorageException.invalidRequest("이미지 파일만 업로드할 수 있습니다.");
		}

		ImageMetadataExtractor.ImageSize imageSize = requireValidImage(bytes);

		String storageKey = keyGenerator.generate(resolvedDirectory, originalFilename);
		objectStorage.put(storageKey, bytes, resolvedContentType);

		String viewUrl = createPresignedGetUrl(storageKey, properties.presignGetTtlSeconds());
		long durationMs = elapsedMs(startedAt);

		logUploadComplete(storageKey, resolvedDirectory, resolvedContentType, bytes.length, imageSize, durationMs);

		return new StorageUploadData(
			storageKey,
			viewUrl,
			resolvedContentType,
			bytes.length,
			imageSize.width(),
			imageSize.height());
	}

	public StoragePresignedGetData issueReadUrl(String storageKey, Integer ttlSecondsOrNull) {
		long startedAt = System.nanoTime();

		validator.validateStorageKey(storageKey);

		int ttlSeconds = validator.resolveTtlSeconds(ttlSecondsOrNull, properties.presignGetTtlSeconds());
		String url = createPresignedGetUrl(storageKey, ttlSeconds);

		long durationMs = elapsedMs(startedAt);

		log.info(
			"스토리지 조회 URL 발급 완료 storageKey={} ttlSeconds={} durationMs={}",
			storageKey, ttlSeconds, durationMs);

		return new StoragePresignedGetData(url, ttlSeconds);
	}

	public void deleteImage(String storageKey) {
		long startedAt = System.nanoTime();

		validator.validateStorageKey(storageKey);
		objectStorage.delete(storageKey);

		long durationMs = elapsedMs(startedAt);

		log.info("스토리지 삭제 완료 storageKey={} durationMs={}", storageKey, durationMs);
	}

	private String resolveDirectory(String directory) {
		return keyGenerator.resolveDirectoryOrDefault(directory, DEFAULT_DIRECTORY);
	}

	private ImageMetadataExtractor.ImageSize requireValidImage(byte[] bytes) {
		ImageMetadataExtractor.ImageSize imageSize = ImageMetadataExtractor.tryReadImageSize(bytes);
		if (imageSize.width() == null || imageSize.height() == null) {
			throw StorageException.invalidRequest("이미지 파일 형식이 올바르지 않습니다.");
		}
		return imageSize;
	}

	private boolean isImageContentType(String contentType) {
		return StringUtils.hasText(contentType)
			&& contentType.toLowerCase(Locale.ROOT).startsWith("image/");
	}

	private String createPresignedGetUrl(String storageKey, int ttlSeconds) {
		return objectStorage.createPresignedGetUrl(storageKey, Duration.ofSeconds(ttlSeconds));
	}

	private byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (Exception e) {
			throw StorageException.internalError("파일을 읽는 중 오류가 발생했습니다.", e);
		}
	}

	private String resolveContentType(String contentType) {
		return StringUtils.hasText(contentType) ? contentType : DEFAULT_CONTENT_TYPE;
	}

	private void logUploadComplete(
		String storageKey,
		String directory,
		String contentType,
		long sizeBytes,
		ImageMetadataExtractor.ImageSize imageSize,
		long durationMs) {
		log.info(
			"스토리지 업로드 완료 storageKey={} directory={} contentType={} sizeBytes={} width={} height={} durationMs={}",
			storageKey,
			directory,
			contentType,
			sizeBytes,
			imageSize.width(),
			imageSize.height(),
			durationMs);
	}

	private long elapsedMs(long startedAtNano) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNano);
	}
}
