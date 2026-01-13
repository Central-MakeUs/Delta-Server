package cmc.delta.domain.problem.application.port;

public interface ObjectStorageReader {
	byte[] readBytes(String storageKey);
}
