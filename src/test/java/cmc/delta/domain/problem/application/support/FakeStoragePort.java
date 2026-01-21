package cmc.delta.domain.problem.application.support;

import cmc.delta.global.storage.port.out.StoragePort;
import org.springframework.web.multipart.MultipartFile;

public final class FakeStoragePort implements StoragePort {

	public String lastUploadDirectory;
	public MultipartFile lastUploadedFile;

	public UploadResult nextUploadResult = new UploadResult(
		"s3/key.png",
		"https://view/s3/key.png",
		"image/png",
		123L,
		100,
		200
	);

	@Override
	public String issueReadUrl(String storageKey) {
		return "https://read/" + storageKey;
	}

	@Override
	public UploadResult uploadImage(MultipartFile file, String directory) {
		this.lastUploadedFile = file;
		this.lastUploadDirectory = directory;
		return nextUploadResult;
	}

	@Override
	public void deleteImage(String storageKey) {
		// no-op
	}
}
