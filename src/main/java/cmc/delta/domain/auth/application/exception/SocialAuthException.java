package cmc.delta.domain.auth.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class SocialAuthException extends BusinessException {

	private SocialAuthException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public static SocialAuthException invalidRequest(String message) {
		return new SocialAuthException(ErrorCode.INVALID_REQUEST, message);
	}

	public static SocialAuthException authenticationFailed(String message) {
		return new SocialAuthException(ErrorCode.AUTHENTICATION_FAILED, message);
	}
}
