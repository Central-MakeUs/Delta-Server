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

	public ProblemTypeException(ErrorCode errorCode, String message, Object data) {
		super(errorCode, message, data);
	}

	public static ProblemTypeException notFound() {
		return new ProblemTypeException(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND);
	}

	public static ProblemTypeException invalid(String message) {
		return new ProblemTypeException(ErrorCode.INVALID_REQUEST, message);
	}

	public static ProblemTypeException duplicateName() {
		return new ProblemTypeException(ErrorCode.TYPE_ALREADY_EXISTS, "이미 추가된 유형입니다.");
	}

	public static ProblemTypeException duplicateNameWithExistingId(String existingId) {
		// 기존 리소스 id를 data 필드에 담아 던진다. GlobalExceptionHandler가 이 데이터를 응답 바디로 전달한다.
		return new ProblemTypeException(ErrorCode.TYPE_ALREADY_EXISTS, "이미 추가된 유형입니다.",
			java.util.Map.of("existingTypeId", existingId));
	}
}
