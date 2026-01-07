package cmc.delta.global.storage.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public final class StorageFixtures {

	private StorageFixtures() {}

	public static MultipartFile png(String filename, int width, int height) {
		try {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "png", out);

			return new MockMultipartFile("file", filename, "image/png", out.toByteArray());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static MultipartFile empty(String filename) {
		return new MockMultipartFile("file", filename, "image/png", new byte[0]);
	}

	public static MultipartFile text(String filename, String content) {
		return new MockMultipartFile("file", filename, "text/plain", content.getBytes());
	}

	public static MultipartFile corruptImage(String filename) {
		return new MockMultipartFile("file", filename, "image/png", "not an image".getBytes());
	}
}
