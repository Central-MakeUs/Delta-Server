package cmc.delta.domain.problem.application.common.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemFinalTypeNotFoundException extends BusinessException {
	public ProblemFinalTypeNotFoundException() {
		super(ErrorCode.INVALID_REQUEST, "finalTypeId에 해당하는 유형이 없습니다.");
	}
}
