package cmc.delta.domain.problem.application.worker.exception;

import cmc.delta.domain.problem.application.worker.support.failure.FailureReason;
import cmc.delta.global.error.ErrorCode;

public class ProblemScanNotFoundException extends ProblemScanWorkerException {

	public ProblemScanNotFoundException(Long scanId) {
		super(ErrorCode.PROBLEM_SCAN_NOT_FOUND, FailureReason.SCAN_NOT_FOUND, "scanId=" + scanId);
	}
}
