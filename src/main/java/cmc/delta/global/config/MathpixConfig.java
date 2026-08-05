package cmc.delta.global.config;

import cmc.delta.domain.problem.adapter.out.ocr.mathpix.MathpixProperties;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MathpixProperties.class)
class MathpixConfig {

	private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(3);
	private static final Timeout RESPONSE_TIMEOUT = Timeout.ofSeconds(60);

	/**
	 * 타임아웃이 없으면 OCR 호출이 멈출 때 워커 스레드가 영구히 점유되어
	 * 스캔 처리 전체가 정지한다. Gemini 클라이언트와 같은 방식으로 상한을 둔다.
	 */
	@Bean
	RestClient mathpixRestClient() {
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectTimeout(CONNECT_TIMEOUT)
			.setResponseTimeout(RESPONSE_TIMEOUT)
			.build();

		CloseableHttpClient httpClient = HttpClients.custom()
			.disableAutomaticRetries()
			.setDefaultRequestConfig(requestConfig)
			.build();

		return RestClient.builder()
			.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
			.build();
	}
}
