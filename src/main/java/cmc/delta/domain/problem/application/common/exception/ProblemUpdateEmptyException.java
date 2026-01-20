package cmc.delta.domain.problem.application.common.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemUpdateEmptyException extends BusinessException {
	public ProblemUpdateEmptyException() {
		super(ErrorCode.PROBLEM_UPDATE_EMPTY);
	}
}
