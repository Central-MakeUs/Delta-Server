package cmc.delta.domain.problem.adapter.in.worker.support.failure;

import cmc.delta.domain.problem.adapter.in.worker.exception.ProblemScanWorkerException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AiFailureDecider {

	private static final long DEFAULT_RATE_LIMIT_DELAY_SECONDS = 180L;
	private static final long MIN_RATE_LIMIT_DELAY_SECONDS = 60L;

	public FailureDecision decide(Exception exception) {
		if (exception instanceof ProblemScanWorkerException workerException) {
			return FailureDecision.nonRetryable(workerException.failureReason());
		}

		if (exception instanceof ResourceAccessException) {
			return FailureDecision.retryable(FailureReason.AI_NETWORK_ERROR);
		}

		if (exception instanceof RestClientResponseException restClientResponseException) {
			int statusCode = restClientResponseException.getRawStatusCode();

			if (statusCode == HttpStatus.TOO_MANY_REQUESTS.value()) {
				Long retryAfterSeconds = extractRetryAfterSeconds(restClientResponseException);
				Long delaySeconds = computeRateLimitDelaySeconds(retryAfterSeconds);
				return new FailureDecision(FailureReason.AI_RATE_LIMIT, true, delaySeconds);
			}
			if (statusCode >= 500) {
				return FailureDecision.retryable(FailureReason.AI_CLIENT_5XX);
			}
			if (statusCode >= 400) {
				return FailureDecision.nonRetryable(FailureReason.AI_CLIENT_4XX);
			}
		}

		return FailureDecision.retryable(FailureReason.AI_FAILED);
	}

	private Long extractRetryAfterSeconds(RestClientResponseException restClientResponseException) {
		HttpHeaders headers = restClientResponseException.getResponseHeaders();
		if (headers == null) return null;

		String retryAfterValue = headers.getFirst("Retry-After");
		if (retryAfterValue == null || retryAfterValue.isBlank()) return null;

		try {
			return Long.parseLong(retryAfterValue.trim());
		} catch (NumberFormatException ignore) {
			return null;
		}
	}

	private Long computeRateLimitDelaySeconds(Long retryAfterSeconds) {
		if (retryAfterSeconds == null || retryAfterSeconds <= 0) {
			return DEFAULT_RATE_LIMIT_DELAY_SECONDS;
		}
		return Math.max(retryAfterSeconds, MIN_RATE_LIMIT_DELAY_SECONDS);
	}
}
