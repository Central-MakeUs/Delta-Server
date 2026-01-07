package cmc.delta.global.storage.support;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StorageUploadSource {

	private final String directory;
	private final String contentType;
	private final byte[] bytes;
	private final Integer imageWidth;
	private final Integer imageHeight;

	public long sizeBytes() {
		return bytes == null ? 0 : bytes.length;
	}
}
