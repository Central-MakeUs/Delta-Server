package cmc.delta.domain.problem.application.port.out.storage;

import cmc.delta.global.storage.port.out.StoredObjectStream;

public interface ObjectStorageReader {
	byte[] readBytes(String storageKey);

	StoredObjectStream openStream(String storageKey);
}
