package cmc.delta.global.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

	// 타임아웃 없는 외부 호출은 장애 전파 시 요청 스레드를 무한정 점유한다.
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder
			.connectTimeout(CONNECT_TIMEOUT)
			.readTimeout(READ_TIMEOUT)
			.build();
	}
}
