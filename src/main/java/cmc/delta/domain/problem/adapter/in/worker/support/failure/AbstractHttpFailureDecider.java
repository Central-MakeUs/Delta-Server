package cmc.delta.domain.problem.adapter.in.worker.support.failure;

import cmc.delta.domain.problem.adapter.in.worker.exception.ProblemScanWorkerException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

public abstract class AbstractHttpFailureDecider {

	private static final long DEFAULT_RATE_LIMIT_DELAY_SECONDS = 180L;
	private static final long MIN_RATE_LIMIT_DELAY_SECONDS = 60L;
	private static final long ZERO = 0L;
	private static final int HTTP_STATUS_4XX_START = 400;
	private static final int HTTP_STATUS_5XX_START = 500;
	private static final int HTTP_STATUS_TOO_MANY_REQUESTS = HttpStatus.TOO_MANY_REQUESTS.value();
	private static final String RETRY_AFTER_HEADER = "Retry-After";

	public final FailureDecision decide(Exception exception) {
		if (exception instanceof ProblemScanWorkerException workerException) {
			return FailureDecision.nonRetryable(workerException.failureReason());
		}

		if (exception instanceof ResourceAccessException) {
			return FailureDecision.retryable(networkErrorReason());
		}

		if (exception instanceof RestClientResponseException rest) {
			int statusCode = rest.getRawStatusCode();

			if (statusCode == HTTP_STATUS_TOO_MANY_REQUESTS) {
				Long retryAfterSeconds = extractRetryAfterSeconds(rest);
				Long delaySeconds = computeRateLimitDelaySeconds(retryAfterSeconds);
				return new FailureDecision(rateLimitReason(), true, delaySeconds);
			}
			if (statusCode >= HTTP_STATUS_5XX_START) {
				return FailureDecision.retryable(client5xxReason());
			}
			if (statusCode >= HTTP_STATUS_4XX_START) {
				return FailureDecision.nonRetryable(client4xxReason());
			}
		}

		return FailureDecision.retryable(unknownFailureReason());
	}

	protected abstract FailureReason networkErrorReason();

	protected abstract FailureReason rateLimitReason();

	protected abstract FailureReason client5xxReason();

	protected abstract FailureReason client4xxReason();

	protected abstract FailureReason unknownFailureReason();

	private Long extractRetryAfterSeconds(RestClientResponseException rest) {
		HttpHeaders headers = rest.getResponseHeaders();
		if (headers == null) {
			return null;
		}

		String retryAfterValue = headers.getFirst(RETRY_AFTER_HEADER);
		if (retryAfterValue == null || retryAfterValue.isBlank()) {
			return null;
		}

		try {
			return Long.parseLong(retryAfterValue.trim());
		} catch (NumberFormatException ignore) {
			return null;
		}
	}

	private Long computeRateLimitDelaySeconds(Long retryAfterSeconds) {
		if (retryAfterSeconds == null || retryAfterSeconds <= ZERO) {
			return DEFAULT_RATE_LIMIT_DELAY_SECONDS;
		}
		return Math.max(retryAfterSeconds, MIN_RATE_LIMIT_DELAY_SECONDS);
	}
}
