package cmc.delta.domain.auth.application.support;

import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.logging.TokenAuditLogger;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class TokenFixtures {

	private TokenFixtures() {}

	public static final String DEFAULT_ROLE = "USER";
	public static final String DEFAULT_SESSION_ID = "DEFAULT";

	public static Clock fixedClock() {
		return Clock.fixed(Instant.parse("2026-01-04T00:00:00Z"), ZoneOffset.UTC);
	}

	public static UserPrincipal principal(long userId) {
		return new UserPrincipal(userId, DEFAULT_ROLE);
	}

	public static TokenAuditLogger noopAuditLogger() {
		return new TokenAuditLogger() {
			@Override
			public void invalidateAll(long userId, String sessionId, boolean blacklisted, long blacklistTtlSeconds) {}

			@Override
			public void reissueFailed(long userId, String sessionId, String rotateResult, String errorCode) {}

			@Override
			public void refreshMismatch(long userId, String sessionId, String action, String errorCode) {}

			@Override
			public void blacklistFailed(long userId, String sessionId, String action, String reason) {}
		};
	}
}

