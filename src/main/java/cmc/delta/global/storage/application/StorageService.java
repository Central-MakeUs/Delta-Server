package cmc.delta.global.storage.application;

import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.api.storage.dto.StorageUploadData;
import cmc.delta.global.storage.port.out.ObjectStorage;
import cmc.delta.global.storage.exception.StorageException;
import cmc.delta.global.storage.adapter.out.s3.S3Properties;
import cmc.delta.global.storage.support.ImageMetadataExtractor;
import cmc.delta.global.storage.support.StorageKeyGenerator;
import cmc.delta.global.storage.support.StorageRequestValidator;
import cmc.delta.global.storage.support.StorageUploadSource;
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

		StorageUploadSource source = prepareUploadSource(file, directory);
		String storageKey = keyGenerator.generate(source.getDirectory(), file.getOriginalFilename());

		objectStorage.put(storageKey, source.getBytes(), source.getContentType());

		String viewUrl = issueViewUrl(storageKey);

		long durationMs = elapsedMs(startedAt);

		log.info(
			"스토리지 업로드 완료 storageKey={} directory={} contentType={} sizeBytes={} width={} height={} durationMs={}",
			storageKey,
			source.getDirectory(),
			source.getContentType(),
			source.sizeBytes(),
			source.getImageWidth(),
			source.getImageHeight(),
			durationMs
		);

		return new StorageUploadData(
			storageKey,
			viewUrl,
			source.getContentType(),
			source.sizeBytes(),
			source.getImageWidth(),
			source.getImageHeight()
		);
	}

	public StoragePresignedGetData issueReadUrl(String storageKey, Integer ttlSecondsOrNull) {
		long startedAt = System.nanoTime();

		validator.validateStorageKey(storageKey);

		int ttlSeconds = validator.resolveTtlSeconds(ttlSecondsOrNull, properties.presignGetTtlSeconds());
		String url = objectStorage.createPresignedGetUrl(storageKey, Duration.ofSeconds(ttlSeconds));

		long durationMs = elapsedMs(startedAt);

		log.info(
			"스토리지 조회 URL 발급 완료 storageKey={} ttlSeconds={} durationMs={}",
			storageKey, ttlSeconds, durationMs
		);

		return new StoragePresignedGetData(url, ttlSeconds);
	}

	public void deleteImage(String storageKey) {
		long startedAt = System.nanoTime();

		validator.validateStorageKey(storageKey);
		objectStorage.delete(storageKey);

		long durationMs = elapsedMs(startedAt);

		log.info("스토리지 삭제 완료 storageKey={} durationMs={}", storageKey, durationMs);
	}

	private StorageUploadSource prepareUploadSource(MultipartFile file, String directory) {
		validator.validateUploadFile(file, properties.maxUploadBytes());

		String resolvedDirectory = keyGenerator.resolveDirectoryOrDefault(directory, DEFAULT_DIRECTORY);
		String contentType = resolveContentType(file.getContentType());

		if (!isImage(contentType)) {
			throw StorageException.invalidRequest("이미지 파일만 업로드할 수 있습니다.");
		}

		byte[] bytes = readBytes(file);

		ImageMetadataExtractor.ImageSize imageSize = ImageMetadataExtractor.tryReadImageSize(bytes);
		if (imageSize.width() == null || imageSize.height() == null) {
			throw StorageException.invalidRequest("이미지 파일 형식이 올바르지 않습니다.");
		}

		return new StorageUploadSource(
			resolvedDirectory,
			contentType,
			bytes,
			imageSize.width(),
			imageSize.height()
		);
	}


	private boolean isImage(String contentType) {
		return StringUtils.hasText(contentType)
			&& contentType.toLowerCase(Locale.ROOT).startsWith("image/");
	}


	private String issueViewUrl(String storageKey) {
		Duration ttl = Duration.ofSeconds(properties.presignGetTtlSeconds());
		return objectStorage.createPresignedGetUrl(storageKey, ttl);
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

	private long elapsedMs(long startedAtNano) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNano);
	}
}
