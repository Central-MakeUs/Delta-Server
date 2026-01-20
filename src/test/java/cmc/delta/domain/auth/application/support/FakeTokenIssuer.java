package cmc.delta.domain.auth.application.support;

import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.global.config.security.principal.UserPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class FakeTokenIssuer implements TokenIssuer {

	private final Clock clock;
	private final Duration accessTtl;

	public FakeTokenIssuer(Clock clock, Duration accessTtl) {
		this.clock = clock;
		this.accessTtl = accessTtl;
	}

	@Override
	public IssuedTokens issue(UserPrincipal principal) {
		String jti = UUID.randomUUID().toString();

		Instant now = clock.instant();
		Instant exp = now.plus(accessTtl);

		String access = "at:" + principal.userId() + ":" + jti + ":" + exp.getEpochSecond();
		String refresh = "rt:" + principal.userId() + ":" + UUID.randomUUID() + ":" + now.getEpochSecond();

		return new IssuedTokens(access, refresh, "Bearer");
	}

	@Override
	public Long extractUserIdFromRefreshToken(String refreshToken) {
		String[] parts = refreshToken.split(":");
		return Long.parseLong(parts[1]);
	}

	@Override
	public String extractJtiFromAccessToken(String accessToken) {
		String[] parts = accessToken.split(":");
		return parts[2];
	}

	@Override
	public Duration remainingAccessTtl(String accessToken) {
		String[] parts = accessToken.split(":");
		long expEpochSecond = Long.parseLong(parts[3]);

		long nowEpochSecond = clock.instant().getEpochSecond();
		long remain = expEpochSecond - nowEpochSecond;

		return Duration.ofSeconds(Math.max(remain, 0));
	}
}
