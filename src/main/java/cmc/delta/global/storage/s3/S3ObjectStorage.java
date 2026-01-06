package cmc.delta.global.storage.s3;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import cmc.delta.global.storage.ObjectStorage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3ObjectStorage implements ObjectStorage {

	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
	private static final String PATH_SEPARATOR = "/";
	private static final String DOT = ".";

	private final S3Properties properties;
	private final S3Client s3Client;
	private final S3Presigner s3Presigner;

	@Override
	public StoredObject upload(UploadRequest request) {
		validateUploadRequest(request);

		String storageKey = createStorageKey(request.directory(), request.originalFilename());
		String contentType = normalizeContentType(request.contentType());

		ImageSize imageSize = tryReadImageSize(request.bytes());

		try {
			PutObjectRequest put = PutObjectRequest.builder()
				.bucket(properties.bucket())
				.key(storageKey)
				.contentType(contentType)
				.build();

			s3Client.putObject(put, RequestBody.fromBytes(request.bytes()));

			return new StoredObject(
				storageKey,
				contentType,
				request.bytes().length,
				imageSize.width(),
				imageSize.height()
			);
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "S3 업로드에 실패했습니다.");
		}
	}

	@Override
	public String generateReadUrl(String storageKey, Duration ttl) {
		if (!StringUtils.hasText(storageKey)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "storageKey가 비어있습니다.");
		}

		Duration effectiveTtl = (ttl == null)
			? Duration.ofSeconds(properties.presignGetTtlSeconds())
			: ttl;

		try {
			GetObjectRequest get = GetObjectRequest.builder()
				.bucket(properties.bucket())
				.key(storageKey)
				.build();

			GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
				.signatureDuration(effectiveTtl)
				.getObjectRequest(get)
				.build();

			return s3Presigner.presignGetObject(presign).url().toString();
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Presigned URL 생성에 실패했습니다.");
		}
	}

	@Override
	public void deleteObject(String storageKey) {
		if (!StringUtils.hasText(storageKey)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "storageKey가 비어있습니다.");
		}

		try {
			DeleteObjectRequest delete = DeleteObjectRequest.builder()
				.bucket(properties.bucket())
				.key(storageKey)
				.build();

			s3Client.deleteObject(delete);
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "S3 삭제에 실패했습니다.");
		}
	}

	private void validateUploadRequest(UploadRequest request) {
		if (request == null || request.bytes() == null || request.bytes().length == 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "업로드할 파일이 비어있습니다.");
		}
		if (request.bytes().length > properties.maxUploadBytes()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "파일 용량이 너무 큽니다.");
		}
	}

	private String createStorageKey(String directory, String originalFilename) {
		String prefix = normalizeDirectory(properties.keyPrefix());
		String dir = normalizeDirectory(directory);

		String extension = extractExtension(originalFilename);
		String randomName = UUID.randomUUID().toString().replace("-", "");
		String filename = (extension == null) ? randomName : randomName + DOT + extension;

		if (!StringUtils.hasText(dir)) {
			return prefix + PATH_SEPARATOR + filename;
		}
		return prefix + PATH_SEPARATOR + dir + PATH_SEPARATOR + filename;
	}

	private String normalizeDirectory(String directory) {
		if (!StringUtils.hasText(directory)) {
			return "";
		}
		String d = directory.trim();
		while (d.startsWith(PATH_SEPARATOR)) d = d.substring(1);
		while (d.endsWith(PATH_SEPARATOR)) d = d.substring(0, d.length() - 1);
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

	private String normalizeContentType(String contentType) {
		return StringUtils.hasText(contentType) ? contentType : DEFAULT_CONTENT_TYPE;
	}

	private ImageSize tryReadImageSize(byte[] bytes) {
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			if (image == null) {
				return ImageSize.empty();
			}
			return new ImageSize(image.getWidth(), image.getHeight());
		} catch (Exception ignored) {
			return ImageSize.empty();
		}
	}

	private record ImageSize(Integer width, Integer height) {
		static ImageSize empty() {
			return new ImageSize(null, null);
		}
	}
}
