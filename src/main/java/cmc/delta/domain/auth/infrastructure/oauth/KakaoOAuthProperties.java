package cmc.delta.domain.auth.infrastructure.oauth;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 카카오 OAuth 연동 설정(clientId/secret/redirectUri/timeout)을 바인딩한다. */
@Validated
@ConfigurationProperties(prefix = "oauth.kakao")
public record KakaoOAuthProperties(
	@NotBlank
	String clientId,
	String clientSecret,
	@NotBlank
	String redirectUri,
	@Min(100)
	int connectTimeoutMs,
	@Min(100)
	int readTimeoutMs) {
}
