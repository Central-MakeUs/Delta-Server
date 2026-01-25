package cmc.delta.domain.curriculum.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemTypeException extends BusinessException {

	public ProblemTypeException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ProblemTypeException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public static ProblemTypeException notFound() {
		return new ProblemTypeException(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND);
	}

	public static ProblemTypeException invalid(String message) {
		return new ProblemTypeException(ErrorCode.INVALID_REQUEST, message);
	}

	public static ProblemTypeException duplicateName() {
		return new ProblemTypeException(ErrorCode.INVALID_REQUEST, "이미 추가된 유형입니다.");
	}
}
