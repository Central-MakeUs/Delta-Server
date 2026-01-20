package cmc.delta.domain.problem.adapter.in.worker.exception;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.global.error.ErrorCode;

public class ProblemScanNotFoundException extends ProblemScanWorkerException {

	public ProblemScanNotFoundException(Long scanId) {
		super(ErrorCode.PROBLEM_SCAN_NOT_FOUND, FailureReason.SCAN_NOT_FOUND, "scanId=" + scanId);
	}
}
