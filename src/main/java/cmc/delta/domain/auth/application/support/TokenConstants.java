package cmc.delta.domain.auth.application.support;

import java.time.Duration;

public final class TokenConstants {

    private TokenConstants() {}

    public static final String DEFAULT_SESSION_ID = "DEFAULT";
    public static final Duration DEFAULT_REFRESH_TTL = Duration.ofDays(14);
}
