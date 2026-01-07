package cmc.delta.global.storage.s3;

import cmc.delta.global.storage.ObjectStorage;
import cmc.delta.global.storage.StorageException;
import cmc.delta.global.storage.support.StorageRequestValidator;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ObjectStorage implements ObjectStorage {

	private final S3Properties properties;
	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final StorageRequestValidator validator;

	@Override
	public void put(String storageKey, byte[] bytes, String contentType) {
		validator.validateStorageKey(storageKey);

		execute("S3 업로드", () -> {
			PutObjectRequest put = PutObjectRequest.builder()
				.bucket(properties.bucket())
				.key(storageKey)
				.contentType(contentType)
				.build();

			s3Client.putObject(put, RequestBody.fromBytes(bytes));

			// 바이트/URL/원본파일명 등 민감정보는 로깅 금지
			log.debug("S3 업로드 완료 storageKey={} sizeBytes={}", storageKey, (bytes == null ? 0 : bytes.length));
			return null;
		});
	}

	@Override
	public String createPresignedGetUrl(String storageKey, Duration ttl) {
		validator.validateStorageKey(storageKey);
		validateTtl(ttl);

		return execute("Presigned URL 생성", () -> {
			GetObjectRequest get = GetObjectRequest.builder()
				.bucket(properties.bucket())
				.key(storageKey)
				.build();

			GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
				.signatureDuration(ttl)
				.getObjectRequest(get)
				.build();

			String url = s3Presigner.presignGetObject(presign).url().toString();

			// URL 자체는 민감정보이므로 로깅 금지
			log.debug("S3 조회 URL 생성 완료 storageKey={} ttlSeconds={}", storageKey, ttl.toSeconds());
			return url;
		});
	}

	@Override
	public void delete(String storageKey) {
		validator.validateStorageKey(storageKey);

		execute("S3 삭제", () -> {
			DeleteObjectRequest delete = DeleteObjectRequest.builder()
				.bucket(properties.bucket())
				.key(storageKey)
				.build();

			s3Client.deleteObject(delete);

			log.debug("S3 삭제 완료 storageKey={}", storageKey);
			return null;
		});
	}

	private void validateTtl(Duration ttl) {
		if (ttl == null || ttl.isZero() || ttl.isNegative()) {
			throw StorageException.invalidRequest("ttl이 올바르지 않습니다.");
		}
	}

	private <T> T execute(String action, Supplier<T> supplier) {
		try {
			return supplier.get();
		} catch (StorageException e) {
			throw e;
		} catch (Exception e) {
			throw StorageException.internalError(action + "에 실패했습니다.", e);
		}
	}
}
