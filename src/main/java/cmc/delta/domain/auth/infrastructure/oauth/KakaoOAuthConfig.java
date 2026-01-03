package cmc.delta.domain.auth.infrastructure.oauth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/** 카카오 OAuth 통신용 RestTemplate과 설정 바인딩을 구성한다. */
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
}
