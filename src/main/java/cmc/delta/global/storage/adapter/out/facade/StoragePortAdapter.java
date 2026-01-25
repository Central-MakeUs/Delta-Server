package cmc.delta.global.storage.adapter.out.facade;

import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.api.storage.dto.StorageUploadData;
import cmc.delta.global.storage.application.StorageService;
import cmc.delta.global.storage.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class StoragePortAdapter implements StoragePort {

	private final StorageService storageService;

	@Override
	public String issueReadUrl(String storageKey) {
		StoragePresignedGetData presigned = storageService.issueReadUrl(storageKey, null);
		return presigned.url();
	}

	@Override
	public UploadResult uploadImage(MultipartFile file, String directory) {
		StorageUploadData data = storageService.uploadImage(file, directory);
		return new UploadResult(
			data.storageKey(),
			data.viewUrl(),
			data.contentType(),
			data.sizeBytes(),
			data.width(),
			data.height());
	}

	@Override
	public void deleteImage(String storageKey) {
		storageService.deleteImage(storageKey);
	}
}
