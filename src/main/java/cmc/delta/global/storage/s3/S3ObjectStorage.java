package cmc.delta.global.storage.s3;

import cmc.delta.global.storage.ObjectStorage;
import cmc.delta.global.storage.StorageException;
import java.time.Duration;
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

	private final S3Properties properties;
	private final S3Client s3Client;
	private final S3Presigner s3Presigner;

	@Override
	public void put(String storageKey, byte[] bytes, String contentType) {
		if (!StringUtils.hasText(storageKey)) {
			throw StorageException.invalidRequest("storageKey가 비어있습니다.");
		}
		try {
			PutObjectRequest put = PutObjectRequest.builder()
				.bucket(properties.bucket())
				.key(storageKey)
				.contentType(contentType)
				.build();

			s3Client.putObject(put, RequestBody.fromBytes(bytes));
		} catch (Exception e) {
			throw StorageException.internalError("S3 업로드에 실패했습니다.", e);
		}
	}

	@Override
	public String createPresignedGetUrl(String storageKey, Duration ttl) {
		if (!StringUtils.hasText(storageKey)) {
			throw StorageException.invalidRequest("storageKey가 비어있습니다.");
		}
		if (ttl == null || ttl.isZero() || ttl.isNegative()) {
			throw StorageException.invalidRequest("ttl이 올바르지 않습니다.");
		}

		try {
			GetObjectRequest get = GetObjectRequest.builder()
				.bucket(properties.bucket())
				.key(storageKey)
				.build();

			GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
				.signatureDuration(ttl)
				.getObjectRequest(get)
				.build();

			return s3Presigner.presignGetObject(presign).url().toString();
		} catch (Exception e) {
			throw StorageException.internalError("Presigned URL 생성에 실패했습니다.", e);
		}
	}

	@Override
	public void delete(String storageKey) {
		if (!StringUtils.hasText(storageKey)) {
			throw StorageException.invalidRequest("storageKey가 비어있습니다.");
		}
		try {
			DeleteObjectRequest delete = DeleteObjectRequest.builder()
				.bucket(properties.bucket())
				.key(storageKey)
				.build();

			s3Client.deleteObject(delete);
		} catch (Exception e) {
			throw StorageException.internalError("S3 삭제에 실패했습니다.", e);
		}
	}
}
