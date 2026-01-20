package cmc.delta.domain.problem.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemFinalTypeNotFoundException extends BusinessException {

	public ProblemFinalTypeNotFoundException() {
		super(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND);
	}
}
