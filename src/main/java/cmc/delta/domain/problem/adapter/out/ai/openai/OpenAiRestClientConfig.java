package cmc.delta.domain.problem.adapter.out.ai.openai;

import cmc.delta.global.config.http.ExternalHttpClientFactory;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiRestClientConfig {

	private static final Timeout RESPONSE_TIMEOUT = Timeout.ofSeconds(90);

	/** Gemini 429 폴백 경로에서만 쓰여 호출량이 적다. */
	private static final int MAX_CONNECTIONS = 8;

	@Bean
	public RestClient openAiRestClient(OpenAiProperties properties) {
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
			ExternalHttpClientFactory.create(RESPONSE_TIMEOUT, MAX_CONNECTIONS));

		return RestClient.builder()
			.baseUrl(properties.baseUrl())
			.requestFactory(requestFactory)
			.build();
	}
}
