package cmc.delta.domain.user.adapter.in.support;

import cmc.delta.global.storage.exception.StorageException;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MultipartFileReader {

	public byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw StorageException.internalError("파일을 읽는 중 오류가 발생했습니다.", e);
		}
	}
}
