package cmc.delta.domain.auth.application.service.token;

import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cmc.delta.domain.auth.application.exception.TokenException;
import cmc.delta.domain.auth.application.port.in.token.TokenCommandUseCase;
import cmc.delta.domain.auth.application.port.out.AccessBlacklistStore;
import cmc.delta.domain.auth.application.port.out.RefreshTokenStore;
import cmc.delta.domain.auth.application.port.out.RefreshTokenStore.RotationResult;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.auth.application.support.AuthPrincipalFactory;
import cmc.delta.domain.auth.application.support.RefreshTokenHasher;
import cmc.delta.domain.auth.application.support.TokenConstants;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.logging.TokenAuditLogger;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenCommandUseCase {

	private static final String DEFAULT_SESSION_ID = TokenConstants.DEFAULT_SESSION_ID;
	private static final Duration DEFAULT_REFRESH_TTL = TokenConstants.DEFAULT_REFRESH_TTL;
	private static final long ZERO_TTL_SECONDS = 0L;
	private static final String ACTION_LOGOUT = "logout";
	private static final String ACTION_INVALIDATE_ALL = "invalidateAll";
	private static final String ISSUE_REFRESH_FAILED_MESSAGE = "리프레시 토큰 발급에 실패했습니다.";
	private static final String REISSUE_REFRESH_FAILED_MESSAGE = "리프레시 토큰 재발급에 실패했습니다.";

	private final TokenIssuer tokenIssuer;
	private final RefreshTokenStore refreshTokenStore;
	private final AccessBlacklistStore accessBlacklistStore;
	private final TokenAuditLogger auditLogger;

	@Override
	public TokenIssuer.IssuedTokens issue(UserPrincipal principal) {
		TokenIssuer.IssuedTokens tokens = tokenIssuer.issue(principal);

		String refreshToken = requireText(tokens.refreshToken(), ISSUE_REFRESH_FAILED_MESSAGE);
		saveRefreshHash(principal.userId(), refreshToken);

		return tokens;
	}

	@Override
	public TokenIssuer.IssuedTokens reissue(String refreshToken) {
		requireProvided(refreshToken, ErrorCode.REFRESH_TOKEN_REQUIRED);

		Long userId = tokenIssuer.extractUserIdFromRefreshToken(refreshToken);
		String expectedHash = RefreshTokenHasher.sha256(refreshToken);

		TokenIssuer.IssuedTokens newTokens = tokenIssuer.issue(AuthPrincipalFactory.principalOf(userId));

		String newRefreshToken = requireText(newTokens.refreshToken(), REISSUE_REFRESH_FAILED_MESSAGE);
		String newHash = RefreshTokenHasher.sha256(newRefreshToken);

		rotateRefreshOrThrow(userId, expectedHash, newHash);

		return newTokens;
	}

	@Override
	public void logout(long userId, String accessToken, String refreshToken) {
		requireProvided(accessToken, ErrorCode.TOKEN_REQUIRED);
		requireProvided(refreshToken, ErrorCode.REFRESH_TOKEN_REQUIRED);

		String expectedHash = RefreshTokenHasher.sha256(refreshToken);
		RotationResult check = refreshTokenStore.refreshRotate(
			userId,
			DEFAULT_SESSION_ID,
			expectedHash,
			expectedHash,
			DEFAULT_REFRESH_TTL);

		if (check == RotationResult.MISMATCH) {
			auditLogger.refreshMismatch(
				userId,
				DEFAULT_SESSION_ID,
				ACTION_LOGOUT,
				ErrorCode.INVALID_REFRESH_TOKEN.code());
			throw new TokenException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		blacklistAccessIfPossible(userId, accessToken, true, ACTION_LOGOUT);
		refreshTokenStore.refreshDelete(userId, DEFAULT_SESSION_ID);
	}

	@Override
	public void invalidateAll(long userId, String accessTokenOrNull) {
		refreshTokenStore.refreshDelete(userId, DEFAULT_SESSION_ID);

		BlacklistResult br = blacklistAccessIfPossible(
			userId,
			accessTokenOrNull,
			false,
			ACTION_INVALIDATE_ALL);

		auditLogger.invalidateAll(userId, DEFAULT_SESSION_ID, br.blacklisted(), br.ttlSeconds());
	}

	private void saveRefreshHash(long userId, String refreshToken) {
		String refreshHash = RefreshTokenHasher.sha256(refreshToken);
		refreshTokenStore.refreshSave(
			userId,
			DEFAULT_SESSION_ID,
			refreshHash,
			DEFAULT_REFRESH_TTL);
	}

	private void rotateRefreshOrThrow(long userId, String expectedHash, String newHash) {
		RotationResult result = refreshTokenStore.refreshRotate(
			userId,
			DEFAULT_SESSION_ID,
			expectedHash,
			newHash,
			DEFAULT_REFRESH_TTL);

		if (result != RotationResult.ROTATED) {
			auditLogger.reissueFailed(
				userId,
				DEFAULT_SESSION_ID,
				result.name(),
				ErrorCode.INVALID_REFRESH_TOKEN.code());
			throw new TokenException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
	}

	private BlacklistResult blacklistAccessIfPossible(long userId, String accessTokenOrNull, boolean required,
		String action) {
		if (!StringUtils.hasText(accessTokenOrNull)) {
			if (required)
				throw new TokenException(ErrorCode.TOKEN_REQUIRED);
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
				auditLogger.blacklistFailed(
					userId,
					DEFAULT_SESSION_ID,
					action,
					e.getClass().getSimpleName());
			if (required)
				throw e;
			return BlacklistResult.notBlacklisted();
		}
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
			return new BlacklistResult(false, ZERO_TTL_SECONDS);
		}
	}
}
