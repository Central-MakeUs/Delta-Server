package cmc.delta.domain.user.application.port.out;

import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.api.storage.dto.StorageUploadData;

public interface ProfileImageStoragePort {
	StorageUploadData uploadImage(byte[] bytes, String contentType, String originalFilename, String directory);

	StoragePresignedGetData issueReadUrl(String storageKey, Integer ttlSecondsOrNull);

	void deleteImage(String storageKey);
}
