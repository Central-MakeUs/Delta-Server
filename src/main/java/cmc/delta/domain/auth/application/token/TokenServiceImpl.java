package cmc.delta.domain.auth.application.token;

import cmc.delta.domain.auth.application.port.AccessBlacklistStore;
import cmc.delta.domain.auth.application.port.RefreshTokenStore;
import cmc.delta.domain.auth.application.port.RefreshTokenStore.RotationResult;
import cmc.delta.domain.auth.application.port.TokenIssuer;
import cmc.delta.domain.auth.application.token.exception.TokenException;
import cmc.delta.domain.auth.application.token.hash.RefreshTokenHasher;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
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

	@Override
	public TokenIssuer.IssuedTokens issue(UserPrincipal principal) {
		TokenIssuer.IssuedTokens tokens = tokenIssuer.issue(principal);

		String refreshToken = requireText(tokens.refreshToken(), "리프레시 토큰 발급에 실패했습니다.");
		String refreshHash = RefreshTokenHasher.sha256(refreshToken);

		refreshTokenStore.refreshSave(principal.userId(), DEFAULT_SESSION_ID, refreshHash, DEFAULT_REFRESH_TTL);
		return tokens;
	}

	@Override
	public TokenIssuer.IssuedTokens reissue(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			throw new TokenException(ErrorCode.REFRESH_TOKEN_REQUIRED);
		}

		Long userId = tokenIssuer.extractUserIdFromRefreshToken(refreshToken);
		String expectedHash = RefreshTokenHasher.sha256(refreshToken);

		UserPrincipal principal = new UserPrincipal(userId, DEFAULT_ROLE_FOR_DEV);
		TokenIssuer.IssuedTokens newTokens = tokenIssuer.issue(principal);

		String newRefreshToken = requireText(newTokens.refreshToken(), "리프레시 토큰 재발급에 실패했습니다.");
		String newHash = RefreshTokenHasher.sha256(newRefreshToken);

		RotationResult result = refreshTokenStore.refreshRotate(
			userId, DEFAULT_SESSION_ID, expectedHash, newHash, DEFAULT_REFRESH_TTL);

		if (result != RotationResult.ROTATED) {
			throw new TokenException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		return newTokens;
	}

	@Override
	public void logout(long userId, String accessToken, String refreshToken) {
		if (!StringUtils.hasText(accessToken)) {
			throw new TokenException(ErrorCode.TOKEN_REQUIRED);
		}
		if (!StringUtils.hasText(refreshToken)) {
			throw new TokenException(ErrorCode.REFRESH_TOKEN_REQUIRED);
		}

		String expectedHash = RefreshTokenHasher.sha256(refreshToken);
		RotationResult check = refreshTokenStore.refreshRotate(
			userId, DEFAULT_SESSION_ID, expectedHash, expectedHash, DEFAULT_REFRESH_TTL);

		if (check == RotationResult.MISMATCH) {
			throw new TokenException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		String jti = tokenIssuer.extractJtiFromAccessToken(accessToken);
		Duration ttl = tokenIssuer.remainingAccessTtl(accessToken);
		if (!ttl.isNegative() && !ttl.isZero()) {
			accessBlacklistStore.blacklist(jti, ttl);
		}

		refreshTokenStore.refreshDelete(userId, DEFAULT_SESSION_ID);
	}

	@Override
	public void invalidateAll(long userId, String accessTokenOrNull) {
		refreshTokenStore.refreshDelete(userId, DEFAULT_SESSION_ID);

		if (StringUtils.hasText(accessTokenOrNull)) {
			String jti = tokenIssuer.extractJtiFromAccessToken(accessTokenOrNull);
			Duration ttl = tokenIssuer.remainingAccessTtl(accessTokenOrNull);
			if (!ttl.isNegative() && !ttl.isZero()) {
				accessBlacklistStore.blacklist(jti, ttl);
			}
		}
	}

	private String requireText(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new TokenException(ErrorCode.INTERNAL_ERROR, message);
		}
		return value;
	}
}
