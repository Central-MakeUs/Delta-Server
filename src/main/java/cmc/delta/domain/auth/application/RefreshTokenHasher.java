package cmc.delta.domain.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Refresh 토큰을 SHA-256으로 해싱한다. */
public final class RefreshTokenHasher {

    private RefreshTokenHasher() {}

    public static String sha256(String raw) {
        // raw 토큰을 SHA-256 해시(hex)로 변환한다.
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("refresh token hashing failed", e);
        }
    }

    private static String toHex(byte[] bytes) {
        // byte 배열을 hex 문자열로 만든다.
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
