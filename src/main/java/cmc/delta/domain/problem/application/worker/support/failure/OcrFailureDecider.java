package cmc.delta.domain.problem.application.worker.support.failure;

import cmc.delta.domain.problem.application.worker.exception.ProblemScanWorkerException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OcrFailureDecider {

	public FailureDecision decide(Exception exception) {
		if (exception instanceof ProblemScanWorkerException workerException) {
			return FailureDecision.nonRetryable(workerException.failureReason());
		}

		if (exception instanceof ResourceAccessException) {
			return FailureDecision.retryable(FailureReason.OCR_NETWORK_ERROR);
		}

		if (exception instanceof RestClientResponseException restClientResponseException) {
			int statusCode = restClientResponseException.getRawStatusCode();

			if (statusCode == HttpStatus.TOO_MANY_REQUESTS.value()) {
				return FailureDecision.retryable(FailureReason.OCR_RATE_LIMIT);
			}
			if (statusCode >= 500) {
				return FailureDecision.retryable(FailureReason.OCR_CLIENT_5XX);
			}
			if (statusCode >= 400) {
				return FailureDecision.nonRetryable(FailureReason.OCR_CLIENT_4XX);
			}
		}

		return FailureDecision.retryable(FailureReason.OCR_FAILED);
	}
}
