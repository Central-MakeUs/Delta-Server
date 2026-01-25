package cmc.delta.domain.auth.adapter.out.oauth.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.apple")
public record AppleOAuthProperties(
	String clientId, // Services ID (Identifier)
	String teamId, // Apple Developer Team ID
	String keyId, // Sign in with Apple Key ID
	String privateKey, // p8 내용을 그대로 넣거나(권장X) 별도 로딩해서 주입
	String redirectUri, // Apple Developer Console Return URL과 100% 동일
	long connectTimeoutMs,
	long readTimeoutMs) {
}
