package cmc.delta.domain.problem.adapter.in.worker.support.failure;

public record FailureDecision(
	FailureReason reasonCode,
	boolean retryable,
	Long retryAfterSeconds
) {
	public static FailureDecision nonRetryable(FailureReason reasonCode) {
		return new FailureDecision(reasonCode, false, null);
	}

	public static FailureDecision retryable(FailureReason reasonCode) {
		return new FailureDecision(reasonCode, true, null);
	}

	public static FailureDecision rateLimited(Long retryAfterSeconds) {
		return new FailureDecision(FailureReason.AI_RATE_LIMIT, true, retryAfterSeconds);
	}
}
