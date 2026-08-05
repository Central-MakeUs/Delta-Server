package cmc.delta.global.config;

import cmc.delta.domain.problem.adapter.out.ocr.mathpix.MathpixProperties;
import cmc.delta.global.config.http.ExternalHttpClientFactory;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MathpixProperties.class)
class MathpixConfig {

	/** 타임아웃이 없으면 OCR 호출이 멈출 때 워커 스레드가 영구히 점유되어 스캔 처리 전체가 정지한다. */
	private static final Timeout RESPONSE_TIMEOUT = Timeout.ofSeconds(60);

	/** OCR 워커 동시성(8)보다 넉넉히 잡아야 스레드가 커넥션 대기로 직렬화되지 않는다. */
	private static final int MAX_CONNECTIONS = 16;

	@Bean
	RestClient mathpixRestClient() {
		return RestClient.builder()
			.requestFactory(new HttpComponentsClientHttpRequestFactory(
				ExternalHttpClientFactory.create(RESPONSE_TIMEOUT, MAX_CONNECTIONS)))
			.build();
	}
}
