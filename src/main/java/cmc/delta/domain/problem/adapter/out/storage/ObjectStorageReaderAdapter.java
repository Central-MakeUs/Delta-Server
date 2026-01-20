package cmc.delta.domain.problem.adapter.out.storage;

import cmc.delta.domain.problem.application.port.out.storage.ObjectStorageReader;
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
