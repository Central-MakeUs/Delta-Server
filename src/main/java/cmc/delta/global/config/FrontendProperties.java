package cmc.delta.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend")
public record FrontendProperties(
    String baseUrl
) {
}
