package cmc.delta.domain.problem.application.common.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemScanNotReadyException extends BusinessException {
	public ProblemScanNotReadyException() {
		super(ErrorCode.PROBLEM_SCAN_NOT_READY);
	}
}
