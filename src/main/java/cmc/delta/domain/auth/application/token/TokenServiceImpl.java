package cmc.delta.domain.auth.application.token;

import cmc.delta.domain.auth.application.port.AccessBlacklistStore;
import cmc.delta.domain.auth.application.port.RefreshTokenStore;
import cmc.delta.domain.auth.application.port.RefreshTokenStore.RotationResult;
import cmc.delta.domain.auth.application.port.TokenIssuer;
import cmc.delta.domain.auth.application.token.exception.TokenException;
import cmc.delta.domain.auth.application.token.hash.RefreshTokenHasher;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.logging.TokenAuditLogger;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

	private static final String DEFAULT_ROLE_FOR_DEV = "USER"; // TODO: role 정책 확정 시 교체
	private static final String DEFAULT_SESSION_ID = "DEFAULT";
	private static final Duration DEFAULT_REFRESH_TTL = Duration.ofDays(14);

	private final TokenIssuer tokenIssuer;
	private final RefreshTokenStore refreshTokenStore;
	private final AccessBlacklistStore accessBlacklistStore;
	private final TokenAuditLogger auditLogger;

	@Override
	public TokenIssuer.IssuedTokens issue(UserPrincipal principal) {
		TokenIssuer.IssuedTokens tokens = tokenIssuer.issue(principal);

		String refreshToken = requireText(tokens.refreshToken(), "리프레시 토큰 발급에 실패했습니다.");
		saveRefreshHash(principal.userId(), refreshToken);

		// ✅ 성공 로그 제거(접근로그로 충분)
		return tokens;
	}

	@Override
	public TokenIssuer.IssuedTokens reissue(String refreshToken) {
		requireProvided(refreshToken, ErrorCode.REFRESH_TOKEN_REQUIRED);

		Long userId = tokenIssuer.extractUserIdFromRefreshToken(refreshToken);
		String expectedHash = RefreshTokenHasher.sha256(refreshToken);

		TokenIssuer.IssuedTokens newTokens = tokenIssuer.issue(principalOf(userId));

		String newRefreshToken = requireText(newTokens.refreshToken(), "리프레시 토큰 재발급에 실패했습니다.");
		String newHash = RefreshTokenHasher.sha256(newRefreshToken);

		rotateRefreshOrThrow(userId, expectedHash, newHash);

		// ✅ 성공 로그 제거(접근로그로 충분)
		return newTokens;
	}

	@Override
	public void logout(long userId, String accessToken, String refreshToken) {
		requireProvided(accessToken, ErrorCode.TOKEN_REQUIRED);
		requireProvided(refreshToken, ErrorCode.REFRESH_TOKEN_REQUIRED);

		String expectedHash = RefreshTokenHasher.sha256(refreshToken);
		RotationResult check = refreshTokenStore.refreshRotate(
			userId, DEFAULT_SESSION_ID, expectedHash, expectedHash, DEFAULT_REFRESH_TTL);

		if (check == RotationResult.MISMATCH) {
			auditLogger.refreshMismatch(userId, DEFAULT_SESSION_ID, "logout", ErrorCode.INVALID_REFRESH_TOKEN.code());
			throw new TokenException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		blacklistAccessIfPossible(userId, accessToken, true, "logout");
		refreshTokenStore.refreshDelete(userId, DEFAULT_SESSION_ID);
	}

	@Override
	public void invalidateAll(long userId, String accessTokenOrNull) {
		refreshTokenStore.refreshDelete(userId, DEFAULT_SESSION_ID);

		BlacklistResult br = blacklistAccessIfPossible(userId, accessTokenOrNull, false, "invalidateAll");

		auditLogger.invalidateAll(userId, DEFAULT_SESSION_ID, br.blacklisted(), br.ttlSeconds());
	}

	private void saveRefreshHash(long userId, String refreshToken) {
		String refreshHash = RefreshTokenHasher.sha256(refreshToken);
		refreshTokenStore.refreshSave(userId, DEFAULT_SESSION_ID, refreshHash, DEFAULT_REFRESH_TTL);
	}

	private void rotateRefreshOrThrow(long userId, String expectedHash, String newHash) {
		RotationResult result = refreshTokenStore.refreshRotate(
			userId, DEFAULT_SESSION_ID, expectedHash, newHash, DEFAULT_REFRESH_TTL);

		if (result != RotationResult.ROTATED) {
			auditLogger.reissueFailed(
				userId, DEFAULT_SESSION_ID, result.name(), ErrorCode.INVALID_REFRESH_TOKEN.code()
			);
			throw new TokenException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
	}

	private BlacklistResult blacklistAccessIfPossible(long userId, String accessTokenOrNull, boolean required, String action) {
		if (!StringUtils.hasText(accessTokenOrNull)) {
			if (required) throw new TokenException(ErrorCode.TOKEN_REQUIRED);
			return BlacklistResult.notBlacklisted();
		}

		try {
			String jti = tokenIssuer.extractJtiFromAccessToken(accessTokenOrNull);
			Duration ttl = tokenIssuer.remainingAccessTtl(accessTokenOrNull);

			if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
				accessBlacklistStore.blacklist(jti, ttl);
				return BlacklistResult.blacklisted(ttl.getSeconds());
			}
			return BlacklistResult.notBlacklisted();

		} catch (RuntimeException e) {
			// 토큰 원문/해시 절대 로그 금지. 예외 메시지도 최소화(클래스명만)
			auditLogger.blacklistFailed(userId, DEFAULT_SESSION_ID, action, e.getClass().getSimpleName());
			if (required) throw e;
			return BlacklistResult.notBlacklisted();
		}
	}

	private UserPrincipal principalOf(long userId) {
		return new UserPrincipal(userId, DEFAULT_ROLE_FOR_DEV);
	}

	private void requireProvided(String value, ErrorCode errorCode) {
		if (!StringUtils.hasText(value)) {
			throw new TokenException(errorCode);
		}
	}

	private String requireText(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new TokenException(ErrorCode.INTERNAL_ERROR, message);
		}
		return value;
	}

	private record BlacklistResult(boolean blacklisted, long ttlSeconds) {
		static BlacklistResult blacklisted(long ttlSeconds) {
			return new BlacklistResult(true, ttlSeconds);
		}

		static BlacklistResult notBlacklisted() {
			return new BlacklistResult(false, 0L);
		}
	}
}
