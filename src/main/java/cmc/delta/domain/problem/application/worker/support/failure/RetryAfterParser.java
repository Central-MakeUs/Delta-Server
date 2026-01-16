package cmc.delta.domain.problem.application.worker.support.failure;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

public final class RetryAfterParser {

	public static final String RETRY_AFTER_HEADER = "Retry-After";

	private RetryAfterParser() {}

	public static Long parseSecondsIf429(RestClientResponseException e) {
		if (e.getRawStatusCode() != 429) return null;

		HttpHeaders headers = e.getResponseHeaders();
		if (headers == null) return null;

		String value = headers.getFirst(RETRY_AFTER_HEADER);
		if (value == null || value.isBlank()) return null;

		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException ignore) {
			return null;
		}
	}
}
