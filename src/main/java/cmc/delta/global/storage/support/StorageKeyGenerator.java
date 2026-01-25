package cmc.delta.global.storage.support;

import cmc.delta.global.storage.adapter.out.s3.S3Properties;
import cmc.delta.global.storage.exception.StorageException;
import jakarta.annotation.PostConstruct;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StorageKeyGenerator {

	private static final String PATH_SEPARATOR = "/";
	private static final String DOT = ".";

	private final S3Properties properties;

	private String normalizedPrefix;

	@PostConstruct
	void init() {
		this.normalizedPrefix = normalizeDirectory(properties.keyPrefix(), "keyPrefix");
	}

	public String resolveDirectoryOrDefault(String directoryOrNull, String defaultDirectory) {
		return StringUtils.hasText(directoryOrNull) ? directoryOrNull : defaultDirectory;
	}

	public String generate(String directory, String originalFilename) {
		String dir = normalizeDirectory(directory, "directory");
		String extension = extractExtension(originalFilename);

		String randomName = UUID.randomUUID().toString().replace("-", "");
		String filename = (extension == null) ? randomName : randomName + DOT + extension;

		return normalizedPrefix + PATH_SEPARATOR + dir + PATH_SEPARATOR + filename;
	}

	private String normalizeDirectory(String directory, String fieldName) {
		if (!StringUtils.hasText(directory)) {
			throw StorageException.invalidRequest(fieldName + "가 비어있습니다.");
		}
		String d = directory.trim();

		while (d.startsWith(PATH_SEPARATOR))
			d = d.substring(1);
		while (d.endsWith(PATH_SEPARATOR))
			d = d.substring(0, d.length() - 1);

		if (!StringUtils.hasText(d)) {
			throw StorageException.invalidRequest(fieldName + "가 비어있습니다.");
		}
		if (d.contains("..") || d.contains("\\") || d.contains("//")) {
			throw StorageException.invalidRequest(fieldName + " 형식이 올바르지 않습니다.");
		}
		return d;
	}

	private String extractExtension(String originalFilename) {
		if (!StringUtils.hasText(originalFilename)) {
			return null;
		}
		int idx = originalFilename.lastIndexOf(DOT);
		if (idx < 0 || idx == originalFilename.length() - 1) {
			return null;
		}
		return originalFilename.substring(idx + 1).toLowerCase(Locale.ROOT);
	}
}
