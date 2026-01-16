package cmc.delta.domain.problem.application.worker.support.failure;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

public class AiFailureDecider {

	public FailureDecision decide(Exception e) {
		ScanFailReasonCode reasonCode = classify(e);
		boolean retryable = isRetryable(e);

		Long retryAfterSeconds = null;
		if (e instanceof RestClientResponseException rre && rre.getRawStatusCode() == 429) {
			retryAfterSeconds = RetryAfterParser.parseSecondsIf429(rre);
		}

		return FailureDecision.of(reasonCode, retryable, retryAfterSeconds);
	}

	private boolean isRetryable(Exception e) {
		if (e instanceof IllegalStateException ise) {
			String msg = ise.getMessage();
			if ("OCR_TEXT_EMPTY".equals(msg)) return false;
			if ("SCAN_NOT_FOUND".equals(msg)) return false;
			return false;
		}

		if (e instanceof ResourceAccessException) return true;

		if (e instanceof RestClientResponseException rre) {
			int status = rre.getRawStatusCode();
			if (status == 429) return true;
			if (status == 408) return true;
			if (status >= 500) return true;
			return false;
		}

		return true;
	}

	private ScanFailReasonCode classify(Exception e) {
		if (e instanceof IllegalStateException ise) {
			String msg = ise.getMessage();
			if ("OCR_TEXT_EMPTY".equals(msg)) return ScanFailReasonCode.OCR_TEXT_EMPTY;
			if ("SCAN_NOT_FOUND".equals(msg)) return ScanFailReasonCode.SCAN_NOT_FOUND;
			return ScanFailReasonCode.ILLEGAL_STATE;
		}

		if (e instanceof RestClientResponseException rre) {
			int status = rre.getRawStatusCode();
			if (status == 429) return ScanFailReasonCode.AI_RATE_LIMIT;
			if (status >= 400 && status < 500) return ScanFailReasonCode.AI_CLIENT_4XX;
			if (status >= 500) return ScanFailReasonCode.AI_CLIENT_5XX;
			return ScanFailReasonCode.AI_CLIENT_ERROR;
		}

		if (e instanceof ResourceAccessException) return ScanFailReasonCode.AI_NETWORK_ERROR;

		return ScanFailReasonCode.AI_FAILED;
	}
}
