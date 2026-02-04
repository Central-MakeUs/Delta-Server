package cmc.delta.domain.problem.adapter.in.worker.support.failure;

public record FailureDecision(
	FailureReason reasonCode,
	boolean retryable,
	Long retryAfterSeconds) {

	private static final Long NO_RETRY_AFTER = null;

	public static FailureDecision nonRetryable(FailureReason reasonCode) {
		return new FailureDecision(reasonCode, false, NO_RETRY_AFTER);
	}

	public static FailureDecision retryable(FailureReason reasonCode) {
		return new FailureDecision(reasonCode, true, NO_RETRY_AFTER);
	}
}
