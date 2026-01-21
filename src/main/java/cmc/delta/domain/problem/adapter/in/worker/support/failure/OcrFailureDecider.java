package cmc.delta.domain.problem.adapter.in.worker.support.failure;

import org.springframework.stereotype.Component;

@Component
public class OcrFailureDecider extends AbstractHttpFailureDecider {

	@Override
	protected FailureReason networkErrorReason() {
		return FailureReason.OCR_NETWORK_ERROR;
	}

	@Override
	protected FailureReason rateLimitReason() {
		return FailureReason.OCR_RATE_LIMIT;
	}

	@Override
	protected FailureReason client5xxReason() {
		return FailureReason.OCR_CLIENT_5XX;
	}

	@Override
	protected FailureReason client4xxReason() {
		return FailureReason.OCR_CLIENT_4XX;
	}

	@Override
	protected FailureReason unknownFailureReason() {
		return FailureReason.OCR_FAILED;
	}
}
