package cmc.delta.domain.problem.application.exception;

import cmc.delta.global.error.ErrorCode;

public class ProblemValidationException extends ProblemException {

	public ProblemValidationException(String message) {
		super(ErrorCode.INVALID_REQUEST, message);
	}

	public ProblemValidationException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ProblemValidationException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
