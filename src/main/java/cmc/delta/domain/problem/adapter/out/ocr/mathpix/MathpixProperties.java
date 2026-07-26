package cmc.delta.domain.problem.adapter.out.ocr.mathpix;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mathpix")
public record MathpixProperties(
	String baseUrl,
	String appId,
	String appKey,
	long connectTimeoutMs,
	long readTimeoutMs) {

	private static final long DEFAULT_CONNECT_TIMEOUT_MS = 3000L;
	private static final long DEFAULT_READ_TIMEOUT_MS = 30000L;

	public MathpixProperties {
		connectTimeoutMs = connectTimeoutMs > 0 ? connectTimeoutMs : DEFAULT_CONNECT_TIMEOUT_MS;
		readTimeoutMs = readTimeoutMs > 0 ? readTimeoutMs : DEFAULT_READ_TIMEOUT_MS;
	}
}
