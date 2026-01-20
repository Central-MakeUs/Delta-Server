package cmc.delta.domain.problem.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemException extends BusinessException {

	public ProblemException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ProblemException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
