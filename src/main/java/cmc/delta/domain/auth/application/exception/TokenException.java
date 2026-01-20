package cmc.delta.domain.auth.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class TokenException extends BusinessException {

	public TokenException(ErrorCode errorCode) {
		super(errorCode);
	}

	public TokenException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
