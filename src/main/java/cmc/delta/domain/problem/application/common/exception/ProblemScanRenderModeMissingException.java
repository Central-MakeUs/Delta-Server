package cmc.delta.domain.problem.application.common.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemScanRenderModeMissingException extends BusinessException {
	public ProblemScanRenderModeMissingException() {
		super(ErrorCode.INTERNAL_ERROR, "스캔 렌더 모드가 누락되었습니다.");
	}
}
