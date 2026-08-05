package cmc.delta.domain.auth.application.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Refresh 토큰을 SHA-256으로 해싱한다. */
public final class RefreshTokenHasher {

	private static final String HASH_ALGORITHM = "SHA-256";

	private RefreshTokenHasher() {}

	public static String sha256(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
			byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("refresh token hashing failed", e);
		}
	}
}
