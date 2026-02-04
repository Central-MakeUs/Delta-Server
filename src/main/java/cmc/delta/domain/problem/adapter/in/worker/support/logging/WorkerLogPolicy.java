package cmc.delta.domain.problem.adapter.in.worker.support.logging;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureDecision;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class WorkerLogPolicy {

	private static final int HTTP_STATUS_4XX_START = 400;
	private static final int HTTP_STATUS_5XX_START = 500;

	public boolean shouldSuppressStacktrace(Exception exception) {
		if (!(exception instanceof RestClientResponseException restClientResponseException)) {
			return false;
		}
		return isClientError(restClientResponseException.getRawStatusCode());
	}

	public String reasonCode(FailureDecision decision) {
		return decision.reasonCode().code();
	}

	private boolean isClientError(int status) {
		return status >= HTTP_STATUS_4XX_START && status < HTTP_STATUS_5XX_START;
	}
}
