package cmc.delta.domain.auth.application.port;

import java.time.Duration;

/** Refresh 토큰을 저장/회수/로테이션한다. */
public interface RefreshTokenStore {

    /** 로그인 시 Refresh 토큰 해시를 저장한다. */
    void save(Long userId, String sessionId, String refreshTokenHash, Duration ttl);

    /** Refresh 토큰을 원자적으로 교체한다(rotate). */
    RotationResult rotate(Long userId, String sessionId, String expectedHash, String newHash, Duration ttl);

    /** 세션의 Refresh 토큰을 삭제한다. */
    void delete(Long userId, String sessionId);

    /** 로테이션 결과를 표현한다. */
    enum RotationResult { ROTATED, NOT_FOUND, MISMATCH }
}
