package cmc.delta.domain.auth.application.port.out;

import java.time.Duration;

/** Refresh 토큰을 저장/회수/로테이션한다. */
public interface RefreshTokenStore {

	void refreshSave(Long userId, String sessionId, String refreshTokenHash, Duration ttl);

	RotationResult refreshRotate(
		Long userId, String sessionId, String expectedHash, String newHash, Duration ttl);

	void refreshDelete(Long userId, String sessionId);

	enum RotationResult {
		ROTATED,
		NOT_FOUND,
		MISMATCH
	}
}
