package cmc.delta.domain.problem.application.worker.exception;

import cmc.delta.domain.problem.application.worker.support.failure.FailureReason;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public abstract class ProblemScanWorkerException extends BusinessException {

	private final FailureReason failureReason;

	protected ProblemScanWorkerException(ErrorCode errorCode, FailureReason failureReason, String message) {
		super(errorCode, message);
		this.failureReason = failureReason;
	}

	public FailureReason failureReason() {
		return failureReason;
	}
}
