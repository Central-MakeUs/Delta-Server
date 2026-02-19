package cmc.delta.domain.auth.application.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Refresh 토큰을 SHA-256으로 해싱한다. */
public final class RefreshTokenHasher {

	private RefreshTokenHasher() {}

	public static String sha256(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			return toHex(digest);
		} catch (Exception e) {
			throw new IllegalStateException("refresh token hashing failed", e);
		}
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			int v = b & 0xFF;
			if (v < 16)
				sb.append('0');
			sb.append(Integer.toHexString(v));
		}
		return sb.toString();
	}
}
