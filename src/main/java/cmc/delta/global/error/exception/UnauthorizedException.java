package cmc.delta.global.error.exception;

import cmc.delta.global.error.ErrorCode;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(ErrorCode.AUTHENTICATION_FAILED);
    }

    public UnauthorizedException(String message) {
        super(ErrorCode.AUTHENTICATION_FAILED, message);
    }
}
