package cmc.delta.global.config;

import cmc.delta.domain.problem.adapter.out.ocr.mathpix.MathpixProperties;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MathpixProperties.class)
class MathpixConfig {

	private static final int MAX_CONNECTIONS = 20;
	private static final long CONNECTION_REQUEST_TIMEOUT_SECONDS = 2L;
	private static final long CONNECTION_TIME_TO_LIVE_MINUTES = 5L;

	@Bean
	RestClient mathpixRestClient(MathpixProperties props) {
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectTimeout(Timeout.ofMilliseconds(props.connectTimeoutMs()))
			.setResponseTimeout(Timeout.ofMilliseconds(props.readTimeoutMs()))
			.setConnectionRequestTimeout(Timeout.ofSeconds(CONNECTION_REQUEST_TIMEOUT_SECONDS))
			.build();

		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setMaxConnTotal(MAX_CONNECTIONS)
			.setMaxConnPerRoute(MAX_CONNECTIONS)
			.setDefaultConnectionConfig(ConnectionConfig.custom()
				.setTimeToLive(TimeValue.ofMinutes(CONNECTION_TIME_TO_LIVE_MINUTES))
				.build())
			.build();

		CloseableHttpClient httpClient = HttpClients.custom()
			.setDefaultRequestConfig(requestConfig)
			.setConnectionManager(connectionManager)
			.build();

		return RestClient.builder()
			.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
			.build();
	}
}
