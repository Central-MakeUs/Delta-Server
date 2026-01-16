package cmc.delta.domain.problem.application.common.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemScanNotFoundException extends BusinessException {
	public ProblemScanNotFoundException() {
		super(ErrorCode.PROBLEM_SCAN_NOT_FOUND);
	}
}
