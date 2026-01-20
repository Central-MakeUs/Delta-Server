package cmc.delta.domain.problem.application.exception;

import cmc.delta.global.error.ErrorCode;

public class ProblemStateException extends ProblemException {

	public ProblemStateException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ProblemStateException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
