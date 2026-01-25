package cmc.delta.domain.problem.application.port.out.storage;

import cmc.delta.domain.problem.application.port.in.support.UploadFile;

public interface ScanImageUploadPort {
	UploadResult uploadImage(UploadFile file, String directory);

	record UploadResult(String storageKey, Integer width, Integer height) {
	}
}
