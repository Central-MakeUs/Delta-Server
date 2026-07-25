package cmc.delta.global.storage.support;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ImageMetadataExtractor {

	private ImageMetadataExtractor() {}

	/**
	 * 이미지 헤더만 읽어 크기를 추출한다.
	 * 전체 픽셀을 BufferedImage로 디코딩하면 업로드 동시성만큼 힙이 치솟기 때문에 헤더 파싱으로 제한한다.
	 */
	public static ImageSize tryReadImageSize(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return ImageSize.empty();
		}
		try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
			return readSizeFromHeader(imageInput);
		} catch (Exception e) {
			log.debug("이미지 크기 추출 실패 sizeBytes={} reason={}", bytes.length, e.getMessage());
			return ImageSize.empty();
		}
	}

	private static ImageSize readSizeFromHeader(ImageInputStream imageInput) throws java.io.IOException {
		Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
		if (!readers.hasNext()) {
			return ImageSize.empty();
		}
		ImageReader reader = readers.next();
		try {
			reader.setInput(imageInput, true, true);
			return new ImageSize(reader.getWidth(0), reader.getHeight(0));
		} finally {
			reader.dispose();
		}
	}

	public record ImageSize(Integer width, Integer height) {
		public static ImageSize empty() {
			return new ImageSize(null, null);
		}
	}
}
