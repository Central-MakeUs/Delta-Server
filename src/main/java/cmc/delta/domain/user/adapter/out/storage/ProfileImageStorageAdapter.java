package cmc.delta.domain.user.adapter.out.storage;

import cmc.delta.domain.user.application.port.out.ProfileImageStoragePort;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.api.storage.dto.StorageUploadData;
import cmc.delta.global.storage.application.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileImageStorageAdapter implements ProfileImageStoragePort {

	private final StorageService storageService;

	@Override
	public StorageUploadData uploadImage(byte[] bytes, String contentType, String originalFilename, String directory) {
		return storageService.uploadImage(bytes, contentType, originalFilename, directory);
	}

	@Override
	public StoragePresignedGetData issueReadUrl(String storageKey, Integer ttlSecondsOrNull) {
		return storageService.issueReadUrl(storageKey, ttlSecondsOrNull);
	}

	@Override
	public void deleteImage(String storageKey) {
		storageService.deleteImage(storageKey);
	}
}
