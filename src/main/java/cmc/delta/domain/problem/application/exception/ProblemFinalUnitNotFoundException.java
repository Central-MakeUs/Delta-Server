package cmc.delta.domain.problem.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemFinalUnitNotFoundException extends BusinessException {

	public ProblemFinalUnitNotFoundException() {
		super(ErrorCode.PROBLEM_FINAL_UNIT_NOT_FOUND);
	}
}
