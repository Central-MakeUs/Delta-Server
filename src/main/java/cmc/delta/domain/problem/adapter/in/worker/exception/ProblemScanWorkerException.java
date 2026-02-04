package cmc.delta.domain.problem.adapter.in.worker.exception;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public abstract class ProblemScanWorkerException extends BusinessException {

	private final FailureReason failureReason;
	private static final String SCAN_ID_PREFIX = "scanId=";

	protected ProblemScanWorkerException(ErrorCode errorCode, FailureReason failureReason, String message) {
		super(errorCode, message);
		this.failureReason = failureReason;
	}

	protected static String scanMessage(Long scanId) {
		return SCAN_ID_PREFIX + scanId;
	}

	public FailureReason failureReason() {
		return failureReason;
	}
}
