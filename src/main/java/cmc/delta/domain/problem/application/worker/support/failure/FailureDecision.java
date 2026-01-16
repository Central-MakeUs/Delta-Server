package cmc.delta.domain.problem.application.worker.support.failure;

public record FailureDecision(
	ScanFailReasonCode reasonCode,
	boolean retryable,
	Long retryAfterSeconds
) {
	public static FailureDecision of(ScanFailReasonCode reasonCode, boolean retryable) {
		return new FailureDecision(reasonCode, retryable, null);
	}

	public static FailureDecision of(ScanFailReasonCode reasonCode, boolean retryable, Long retryAfterSeconds) {
		return new FailureDecision(reasonCode, retryable, retryAfterSeconds);
	}
}
