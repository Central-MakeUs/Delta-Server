package cmc.delta.domain.auth.adapter.out.oauth.google;

import cmc.delta.domain.auth.adapter.out.oauth.client.OAuthClientExceptionMapper;
import cmc.delta.domain.auth.adapter.out.oauth.client.OAuthHttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** 구글 OAuth 통신용 RestTemplate과 공통 OAuthHttpClient를 구성한다. */
@Configuration
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class GoogleOAuthConfig {

	@Bean
	public RestTemplate googleRestTemplate(RestTemplateBuilder builder, GoogleOAuthProperties props) {
		return builder
			.connectTimeout(Duration.ofMillis(props.connectTimeoutMs()))
			.readTimeout(Duration.ofMillis(props.readTimeoutMs()))
			.build();
	}

	@Bean
	public OAuthHttpClient googleOAuthHttpClient(
		@Qualifier("googleRestTemplate")
		RestTemplate googleRestTemplate,
		OAuthClientExceptionMapper exceptionMapper) {
		return new OAuthHttpClient(googleRestTemplate, exceptionMapper);
	}

	@Bean
	public GoogleOAuthClient googleOAuthClient(
		GoogleOAuthProperties properties,
		@Qualifier("googleOAuthHttpClient")
		OAuthHttpClient googleOAuthHttpClient) {
		return new GoogleOAuthClient(properties, googleOAuthHttpClient);
	}
}
