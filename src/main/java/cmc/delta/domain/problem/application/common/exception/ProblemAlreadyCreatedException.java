package cmc.delta.domain.problem.application.common.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemAlreadyCreatedException extends BusinessException {
	public ProblemAlreadyCreatedException() {
		super(ErrorCode.INVALID_REQUEST, "이미 해당 스캔으로 생성된 오답카드가 있습니다.");
	}
}
