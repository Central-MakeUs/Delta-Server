package cmc.delta.domain.problem.adapter.out.storage;

import cmc.delta.domain.problem.application.port.in.support.UploadFile;
import cmc.delta.domain.problem.application.port.out.storage.ScanImageUploadPort;
import cmc.delta.global.storage.port.out.StoragePort;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ScanImageUploadAdapter implements ScanImageUploadPort {

	private final StoragePort storagePort;

	@Override
	public UploadResult uploadImage(UploadFile file, String directory) {
		MultipartFile multipartFile = new ByteArrayMultipartFile(
			"file",
			file.originalFilename(),
			file.contentType(),
			file.bytes()
		);

		StoragePort.UploadResult uploaded = storagePort.uploadImage(multipartFile, directory);
		return new UploadResult(uploaded.storageKey(), uploaded.width(), uploaded.height());
	}

	private static class ByteArrayMultipartFile implements MultipartFile {

		private final String name;
		private final String originalFilename;
		private final String contentType;
		private final byte[] bytes;

		private ByteArrayMultipartFile(
			String name,
			String originalFilename,
			String contentType,
			byte[] bytes
		) {
			this.name = name;
			this.originalFilename = originalFilename;
			this.contentType = contentType;
			this.bytes = (bytes == null) ? new byte[0] : bytes;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getOriginalFilename() {
			return originalFilename;
		}

		@Override
		public String getContentType() {
			return contentType;
		}

		@Override
		public boolean isEmpty() {
			return bytes.length == 0;
		}

		@Override
		public long getSize() {
			return bytes.length;
		}

		@Override
		public byte[] getBytes() {
			return bytes;
		}

		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(bytes);
		}

		@Override
		public void transferTo(Path dest) throws IOException, IllegalStateException {
			Files.write(dest, bytes);
		}

		@Override
		public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
			Files.write(dest.toPath(), bytes);
		}
	}
}
