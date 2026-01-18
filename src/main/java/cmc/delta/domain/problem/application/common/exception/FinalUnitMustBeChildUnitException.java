package cmc.delta.domain.problem.application.common.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class FinalUnitMustBeChildUnitException extends BusinessException {

	public FinalUnitMustBeChildUnitException() {
		super(ErrorCode.PROBLEM_FINAL_UNIT_MUST_BE_CHILD);
	}
}
