package cmc.delta.global.config.http;

import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

/**
 * 외부 API용 HttpClient를 커넥션 풀 크기까지 명시해서 만든다.
 * HttpClient 기본값은 라우트당 5개라, 워커 동시성을 올려도 그 이상은 커넥션 대기로 직렬화된다.
 */
public final class ExternalHttpClientFactory {

	private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(3);

	/** 풀이 고갈되면 기본 3분을 대기하며 워커 스레드가 묶이므로 짧게 끊고 재시도에 맡긴다. */
	private static final Timeout CONNECTION_REQUEST_TIMEOUT = Timeout.ofSeconds(10);

	private static final TimeValue IDLE_CONNECTION_TTL = TimeValue.ofMinutes(5);

	private ExternalHttpClientFactory() {}

	public static CloseableHttpClient create(Timeout responseTimeout, int maxConnections) {
		return HttpClients.custom()
			.disableAutomaticRetries()
			.setConnectionManager(createConnectionManager(maxConnections))
			.setDefaultRequestConfig(createRequestConfig(responseTimeout))
			.evictIdleConnections(IDLE_CONNECTION_TTL)
			.build();
	}

	private static PoolingHttpClientConnectionManager createConnectionManager(int maxConnections) {
		PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
		// 외부 API는 호스트가 하나뿐이라 라우트당 한도와 전체 한도를 같게 둔다.
		manager.setMaxTotal(maxConnections);
		manager.setDefaultMaxPerRoute(maxConnections);
		manager.setDefaultConnectionConfig(ConnectionConfig.custom()
			.setConnectTimeout(CONNECT_TIMEOUT)
			.setValidateAfterInactivity(TimeValue.of(5, TimeUnit.SECONDS))
			.build());
		return manager;
	}

	private static RequestConfig createRequestConfig(Timeout responseTimeout) {
		return RequestConfig.custom()
			.setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
			.setResponseTimeout(responseTimeout)
			.build();
	}
}
