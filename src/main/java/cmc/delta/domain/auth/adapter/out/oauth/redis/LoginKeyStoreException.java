package cmc.delta.domain.auth.adapter.out.oauth.redis;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class LoginKeyStoreException extends BusinessException {

	private LoginKeyStoreException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message, cause);
	}

	public static LoginKeyStoreException saveFailed(Throwable cause) {
		return new LoginKeyStoreException(ErrorCode.INTERNAL_ERROR, "로그인 키 저장에 실패했습니다.", cause);
	}

	public static LoginKeyStoreException readFailed(Throwable cause) {
		return new LoginKeyStoreException(ErrorCode.INTERNAL_ERROR, "로그인 키 조회에 실패했습니다.", cause);
	}
}
