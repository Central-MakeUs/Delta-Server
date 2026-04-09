package cmc.delta.domain.problem.adapter.in.worker.support.failure;

import org.springframework.stereotype.Component;

@Component
public class AiFailureDecider extends AbstractHttpFailureDecider {

	public AiFailureDecider() {
		super(
			FailureReason.AI_NETWORK_ERROR,
			FailureReason.AI_RATE_LIMIT,
			FailureReason.AI_CLIENT_5XX,
			FailureReason.AI_CLIENT_4XX,
			FailureReason.AI_FAILED);
	}
}
