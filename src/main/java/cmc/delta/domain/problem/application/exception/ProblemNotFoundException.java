package cmc.delta.domain.problem.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemNotFoundException extends BusinessException {
	public ProblemNotFoundException() {
		super(ErrorCode.PROBLEM_NOT_FOUND);
	}
}
