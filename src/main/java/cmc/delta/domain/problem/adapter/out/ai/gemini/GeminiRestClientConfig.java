package cmc.delta.domain.problem.adapter.out.ai.gemini;

import cmc.delta.global.config.http.ExternalHttpClientFactory;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiRestClientConfig {

	private static final Timeout RESPONSE_TIMEOUT = Timeout.ofSeconds(90);

	/** 이 빈을 분류·풀이·리포트 클라이언트가 함께 쓰므로 세 워커의 동시성 합보다 넉넉히 잡는다. */
	private static final int MAX_CONNECTIONS = 16;

	@Bean
	public RestClient geminiRestClient(GeminiProperties props) {
		HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory(
			ExternalHttpClientFactory.create(RESPONSE_TIMEOUT, MAX_CONNECTIONS));

		return RestClient.builder()
			.baseUrl(props.baseUrl())
			.requestFactory(rf)
			.build();
	}
}
