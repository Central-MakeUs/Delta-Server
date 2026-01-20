package cmc.delta.domain.problem.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class InvalidProblemCreateRequestException extends BusinessException {
	public InvalidProblemCreateRequestException(String message) {
		super(ErrorCode.INVALID_REQUEST, message);
	}
}
