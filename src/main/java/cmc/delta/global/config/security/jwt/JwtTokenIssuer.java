package cmc.delta.global.config.security.jwt;

import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.global.config.security.principal.UserPrincipal;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** JwtTokenProvider를 이용해 Access/Refresh 토큰 세트를 발급/파싱한다. */
@Component
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuer {

	private static final String TOKEN_TYPE_BEARER = "Bearer";

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public IssuedTokens issue(UserPrincipal principal) {
		String access = jwtTokenProvider.issueAccessToken(principal);
		String refresh = jwtTokenProvider.issueRefreshToken(principal);
		return new IssuedTokens(access, refresh, TOKEN_TYPE_BEARER);
	}

	@Override
	public Long extractUserIdFromRefreshToken(String refreshToken) {
		JwtTokenProvider.ParsedRefreshToken parsed = jwtTokenProvider.parseRefreshTokenOrThrow(refreshToken);
		return parsed.principal().userId();
	}

	@Override
	public String extractJtiFromAccessToken(String accessToken) {
		JwtTokenProvider.ParsedAccessToken parsed = jwtTokenProvider.parseAccessTokenOrThrow(accessToken);
		return parsed.jti();
	}

	@Override
	public Duration remainingAccessTtl(String accessToken) {
		JwtTokenProvider.ParsedAccessToken parsed = jwtTokenProvider.parseAccessTokenOrThrow(accessToken);

		Duration ttl = Duration.between(Instant.now(), parsed.expiresAt());
		if (ttl.isNegative() || ttl.isZero()) {
			return Duration.ZERO;
		}
		return ttl;
	}
}
