package cmc.delta.global.config.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT 설정 값을 바인딩한다. */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String issuer,
        String secretBase64,
        long accessTtlSeconds,
        long refreshTtlSeconds,
        Blacklist blacklist
) {
    /** 블랙리스트 설정 보관. */
    public record Blacklist(boolean enabled) {}
}
