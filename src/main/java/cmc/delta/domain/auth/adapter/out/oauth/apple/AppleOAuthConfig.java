package cmc.delta.domain.auth.adapter.out.oauth.apple;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(AppleOAuthProperties.class)
public class AppleOAuthConfig {

	@Bean
	public RestTemplate appleRestTemplate(RestTemplateBuilder builder, AppleOAuthProperties props) {
		return builder
			.connectTimeout(Duration.ofMillis(props.connectTimeoutMs()))
			.readTimeout(Duration.ofMillis(props.readTimeoutMs()))
			.build();
	}
}
