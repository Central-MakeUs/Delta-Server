package cmc.delta.domain.problem.application.common.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemScanRenderModeMissingException extends BusinessException {
	public ProblemScanRenderModeMissingException() {
		super(ErrorCode.PROBLEM_SCAN_RENDER_MODE_MISSING);
	}
}
