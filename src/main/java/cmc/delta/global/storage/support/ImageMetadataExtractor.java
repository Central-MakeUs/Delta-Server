package cmc.delta.global.storage.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

public final class ImageMetadataExtractor {

	private ImageMetadataExtractor() {}

	public static ImageSize tryReadImageSize(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return ImageSize.empty();
		}
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			if (image == null)
				return ImageSize.empty();
			return new ImageSize(image.getWidth(), image.getHeight());
		} catch (Exception ignored) {
			return ImageSize.empty();
		}
	}

	public record ImageSize(Integer width, Integer height) {
		public static ImageSize empty() {
			return new ImageSize(null, null);
		}
	}
}
