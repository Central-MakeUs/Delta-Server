package cmc.delta.domain.problem.adapter.in.worker.support.failure;

import org.springframework.stereotype.Component;

@Component
public class OcrFailureDecider extends AbstractHttpFailureDecider {

	public OcrFailureDecider() {
		super(
			FailureReason.OCR_NETWORK_ERROR,
			FailureReason.OCR_RATE_LIMIT,
			FailureReason.OCR_CLIENT_5XX,
			FailureReason.OCR_CLIENT_4XX,
			FailureReason.OCR_FAILED);
	}
}
