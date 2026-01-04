package cmc.delta.domain.user.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class UserException extends BusinessException {

	public UserException(ErrorCode errorCode) {
		super(errorCode);
	}

	public UserException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public static UserException invalidRequest() {
		return new UserException(ErrorCode.INVALID_REQUEST);
	}

	public static UserException userNotFound() {
		return new UserException(ErrorCode.USER_NOT_FOUND);
	}

	public static UserException userWithdrawn() {
		return new UserException(ErrorCode.USER_WITHDRAWN);
	}
}
