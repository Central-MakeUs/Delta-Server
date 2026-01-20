package cmc.delta.domain.problem.application.port.out.storage;

public interface ObjectStorageReader {
	byte[] readBytes(String storageKey);
}
