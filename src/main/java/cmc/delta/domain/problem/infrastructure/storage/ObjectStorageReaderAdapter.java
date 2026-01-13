package cmc.delta.domain.problem.infrastructure.storage;

import cmc.delta.domain.problem.application.port.ObjectStorageReader;
import cmc.delta.global.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ObjectStorageReaderAdapter implements ObjectStorageReader {

	private final ObjectStorage objectStorage;

	@Override
	public byte[] readBytes(String storageKey) {
		return objectStorage.readBytes(storageKey);
	}
}
