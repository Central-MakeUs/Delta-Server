package cmc.delta.domain.problem.adapter.out.ai.gemini;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiRestClientConfig {

	@Bean
	public RestClient geminiRestClient(GeminiProperties props) {
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectTimeout(Timeout.ofSeconds(3))
			.setResponseTimeout(Timeout.ofSeconds(90))
			.build();

		CloseableHttpClient httpClient = HttpClients.custom()
			.disableAutomaticRetries()
			.setDefaultRequestConfig(requestConfig)
			.build();

		HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory(httpClient);

		return RestClient.builder()
			.baseUrl(props.baseUrl())
			.requestFactory(rf)
			.build();
	}
}
