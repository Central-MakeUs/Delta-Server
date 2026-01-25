package cmc.delta.domain.auth.adapter.out.oauth.kakao;

import cmc.delta.domain.auth.adapter.out.oauth.client.OAuthClientExceptionMapper;
import cmc.delta.domain.auth.adapter.out.oauth.client.OAuthHttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** 카카오 OAuth 통신용 RestTemplate과 공통 OAuthHttpClient를 구성한다. */
@Configuration
@EnableConfigurationProperties(KakaoOAuthProperties.class)
public class KakaoOAuthConfig {

	@Bean
	public RestTemplate kakaoRestTemplate(RestTemplateBuilder builder, KakaoOAuthProperties props) {
		return builder
			.connectTimeout(Duration.ofMillis(props.connectTimeoutMs()))
			.readTimeout(Duration.ofMillis(props.readTimeoutMs()))
			.build();
	}

	@Bean
	public OAuthHttpClient kakaoOAuthHttpClient(
		@Qualifier("kakaoRestTemplate")
		RestTemplate kakaoRestTemplate,
		OAuthClientExceptionMapper exceptionMapper) {
		return new OAuthHttpClient(kakaoRestTemplate, exceptionMapper);
	}
}
