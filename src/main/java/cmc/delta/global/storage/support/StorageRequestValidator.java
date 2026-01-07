package cmc.delta.global.storage.support;

import cmc.delta.global.storage.StorageException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class StorageRequestValidator {

	private static final int MIN_TTL_SECONDS = 60;

	public void validateUploadFile(MultipartFile file, long maxUploadBytes) {
		if (file == null || file.isEmpty()) {
			throw StorageException.invalidRequest("파일이 비어있습니다.");
		}
		if (maxUploadBytes <= 0) {
			throw StorageException.internalError(
				"maxUploadBytes 설정이 올바르지 않습니다.",
				new IllegalStateException("maxUploadBytes=" + maxUploadBytes)
			);
		}
		if (file.getSize() > maxUploadBytes) {
			throw StorageException.invalidRequest("파일 용량이 너무 큽니다.");
		}
	}

	public void validateStorageKey(String storageKey) {
		if (!StringUtils.hasText(storageKey)) {
			throw StorageException.invalidRequest("storageKey가 비어있습니다.");
		}
		String key = storageKey.trim();
		if (!StringUtils.hasText(key)) {
			throw StorageException.invalidRequest("storageKey가 비어있습니다.");
		}
		// 단순하지만 효과 큰 방어(키 조작/경로 유사 패턴 방지)
		if (key.contains("..") || key.contains("\\")) {
			throw StorageException.invalidRequest("storageKey 형식이 올바르지 않습니다.");
		}
	}

	public int resolveTtlSeconds(Integer ttlSecondsOrNull, int defaultTtlSeconds) {
		int ttlSeconds = (ttlSecondsOrNull == null) ? defaultTtlSeconds : ttlSecondsOrNull;
		if (ttlSeconds < MIN_TTL_SECONDS) {
			throw StorageException.invalidRequest("ttlSeconds는 60 이상이어야 합니다.");
		}
		return ttlSeconds;
	}
}
