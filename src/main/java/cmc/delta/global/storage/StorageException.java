package cmc.delta.global.storage;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class StorageException extends BusinessException {

	private StorageException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	private StorageException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message);
		initCause(cause);
	}

	public static StorageException invalidRequest(String message) {
		return new StorageException(ErrorCode.INVALID_REQUEST, message);
	}

	public static StorageException internalError(String message, Throwable cause) {
		return new StorageException(ErrorCode.INTERNAL_ERROR, message, cause);
	}
}
