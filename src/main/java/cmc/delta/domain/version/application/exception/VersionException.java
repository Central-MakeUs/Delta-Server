package cmc.delta.domain.version.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class VersionException extends BusinessException {

	private static final String INVALID_VERSION_MESSAGE = "버전은 major.minor.patch 형식이어야 합니다.";

	public VersionException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public static VersionException invalidVersion() {
		return new VersionException(ErrorCode.INVALID_REQUEST, INVALID_VERSION_MESSAGE);
	}
}
