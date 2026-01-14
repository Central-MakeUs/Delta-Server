package cmc.delta.domain.problem.application.port.ocr;

public interface ObjectStorageReader {
	byte[] readBytes(String storageKey);
}
