package cmc.delta.domain.problem.application.common.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemScanNotReadyException extends BusinessException {
	public ProblemScanNotReadyException() {
		super(ErrorCode.INVALID_REQUEST, "AI 분석이 완료된 스캔만 오답카드를 생성할 수 있습니다.");
	}
}
