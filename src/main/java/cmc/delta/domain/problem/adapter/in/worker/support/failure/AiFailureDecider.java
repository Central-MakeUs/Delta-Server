package cmc.delta.domain.problem.adapter.in.worker.support.failure;

import org.springframework.stereotype.Component;

@Component
public class AiFailureDecider extends AbstractHttpFailureDecider {

	@Override
	protected FailureReason networkErrorReason() {
		return FailureReason.AI_NETWORK_ERROR;
	}

	@Override
	protected FailureReason rateLimitReason() {
		return FailureReason.AI_RATE_LIMIT;
	}

	@Override
	protected FailureReason client5xxReason() {
		return FailureReason.AI_CLIENT_5XX;
	}

	@Override
	protected FailureReason client4xxReason() {
		return FailureReason.AI_CLIENT_4XX;
	}

	@Override
	protected FailureReason unknownFailureReason() {
		return FailureReason.AI_FAILED;
	}
}
