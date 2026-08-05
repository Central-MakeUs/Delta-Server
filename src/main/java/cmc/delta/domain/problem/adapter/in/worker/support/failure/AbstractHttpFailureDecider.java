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

	private final FailureReason networkErrorReason;
	private final FailureReason rateLimitReason;
	private final FailureReason client5xxReason;
	private final FailureReason client4xxReason;
	private final FailureReason unknownFailureReason;

	protected AbstractHttpFailureDecider(
		FailureReason networkErrorReason,
		FailureReason rateLimitReason,
		FailureReason client5xxReason,
		FailureReason client4xxReason,
		FailureReason unknownFailureReason) {
		this.networkErrorReason = networkErrorReason;
		this.rateLimitReason = rateLimitReason;
		this.client5xxReason = client5xxReason;
		this.client4xxReason = client4xxReason;
		this.unknownFailureReason = unknownFailureReason;
	}

	public final FailureDecision decide(Exception exception) {
		if (exception instanceof ProblemScanWorkerException workerException) {
			return FailureDecision.nonRetryable(workerException.failureReason());
		}
		if (exception instanceof ResourceAccessException) {
			return FailureDecision.retryable(networkErrorReason);
		}
		if (exception instanceof RestClientResponseException rest) {
			return decideByStatusCode(rest);
		}
		return FailureDecision.retryable(unknownFailureReason);
	}

	private FailureDecision decideByStatusCode(RestClientResponseException rest) {
		int statusCode = rest.getRawStatusCode();
		if (isRateLimited(statusCode)) {
			return decideRateLimited(rest);
		}
		if (isServerError(statusCode)) {
			return FailureDecision.retryable(client5xxReason);
		}
		if (isClientError(statusCode)) {
			return FailureDecision.nonRetryable(client4xxReason);
		}
		return FailureDecision.retryable(unknownFailureReason);
	}

	private boolean isRateLimited(int statusCode) {
		return statusCode == HTTP_STATUS_TOO_MANY_REQUESTS;
	}

	private boolean isServerError(int statusCode) {
		return statusCode >= HTTP_STATUS_5XX_START;
	}

	private boolean isClientError(int statusCode) {
		return statusCode >= HTTP_STATUS_4XX_START;
	}

	private FailureDecision decideRateLimited(RestClientResponseException rest) {
		Long delaySeconds = computeRateLimitDelaySeconds(extractRetryAfterSeconds(rest));
		return new FailureDecision(rateLimitReason, true, delaySeconds);
	}

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
