package cmc.delta.domain.problem.adapter.out.ai.gemini;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiRestClientConfig {

	private static final long CONNECT_TIMEOUT_SECONDS = 3L;
	private static final long RESPONSE_TIMEOUT_SECONDS = 90L;
	private static final long CONNECTION_REQUEST_TIMEOUT_SECONDS = 5L;
	private static final long CONNECTION_TIME_TO_LIVE_MINUTES = 5L;
	private static final int MAX_CONNECTIONS_TOTAL = 50;
	private static final int MAX_CONNECTIONS_PER_ROUTE = 20;

	@Bean
	public RestClient geminiRestClient(GeminiProperties props) {
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectTimeout(Timeout.ofSeconds(CONNECT_TIMEOUT_SECONDS))
			.setResponseTimeout(Timeout.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
			.setConnectionRequestTimeout(Timeout.ofSeconds(CONNECTION_REQUEST_TIMEOUT_SECONDS))
			.build();

		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setMaxConnTotal(MAX_CONNECTIONS_TOTAL)
			.setMaxConnPerRoute(MAX_CONNECTIONS_PER_ROUTE)
			.setDefaultConnectionConfig(ConnectionConfig.custom()
				.setTimeToLive(TimeValue.ofMinutes(CONNECTION_TIME_TO_LIVE_MINUTES))
				.build())
			.build();

		CloseableHttpClient httpClient = HttpClients.custom()
			.disableAutomaticRetries()
			.setDefaultRequestConfig(requestConfig)
			.setConnectionManager(connectionManager)
			.build();

		HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory(httpClient);

		return RestClient.builder()
			.baseUrl(props.baseUrl())
			.requestFactory(rf)
			.build();
	}
}
