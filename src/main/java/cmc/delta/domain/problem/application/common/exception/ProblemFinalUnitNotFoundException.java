package cmc.delta.domain.problem.application.common.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemFinalUnitNotFoundException extends BusinessException {
	public ProblemFinalUnitNotFoundException() {
		super(ErrorCode.INVALID_REQUEST, "finalUnitId에 해당하는 단원이 없습니다.");
	}
}
