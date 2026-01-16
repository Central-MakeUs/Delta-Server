package cmc.delta.domain.problem.application.worker.support.logging;

import cmc.delta.domain.problem.application.worker.support.failure.FailureDecision;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class WorkerLogPolicy {

	public boolean shouldSuppressStacktrace(Exception exception) {
		if (exception instanceof RestClientResponseException restClientResponseException) {
			int status = restClientResponseException.getRawStatusCode();
			return status >= 400 && status < 500;
		}
		return false;
	}

	public String reasonCode(FailureDecision decision) {
		return decision.reasonCode().code();
	}
}
