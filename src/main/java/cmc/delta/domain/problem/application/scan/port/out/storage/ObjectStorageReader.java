package cmc.delta.domain.problem.application.scan.port.out.storage;

public interface ObjectStorageReader {
	byte[] readBytes(String storageKey);
}
