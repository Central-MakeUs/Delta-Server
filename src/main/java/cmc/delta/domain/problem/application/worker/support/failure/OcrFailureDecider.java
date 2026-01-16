package cmc.delta.domain.problem.application.worker.support.failure;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

public class OcrFailureDecider {

	public FailureDecision decide(Exception e) {
		ScanFailReasonCode reasonCode = classify(e);
		boolean retryable = isRetryable(e);
		return FailureDecision.of(reasonCode, retryable);
	}

	private boolean isRetryable(Exception e) {
		if (e instanceof IllegalStateException) {
			return false;
		}
		if (e instanceof ResourceAccessException) return true;

		if (e instanceof RestClientResponseException rre) {
			int status = rre.getRawStatusCode();
			if (status == 429) return true;
			if (status >= 500) return true;
			return false;
		}

		return true;
	}

	private ScanFailReasonCode classify(Exception e) {
		if (e instanceof IllegalStateException ise) {
			String msg = ise.getMessage();
			if ("ASSET_NOT_FOUND".equals(msg)) return ScanFailReasonCode.ASSET_NOT_FOUND;
			if ("SCAN_NOT_FOUND".equals(msg)) return ScanFailReasonCode.SCAN_NOT_FOUND;
			return ScanFailReasonCode.ILLEGAL_STATE;
		}

		if (e instanceof RestClientResponseException rre) {
			int status = rre.getRawStatusCode();
			if (status == 429) return ScanFailReasonCode.OCR_RATE_LIMIT;
			if (status >= 400 && status < 500) return ScanFailReasonCode.OCR_CLIENT_4XX;
			if (status >= 500) return ScanFailReasonCode.OCR_CLIENT_5XX;
			return ScanFailReasonCode.OCR_CLIENT_ERROR;
		}

		if (e instanceof ResourceAccessException) return ScanFailReasonCode.OCR_NETWORK_ERROR;

		return ScanFailReasonCode.OCR_FAILED;
	}
}
