package cmc.delta.global.config.security.jwt;

import org.springframework.security.core.AuthenticationException;

import cmc.delta.global.error.ErrorCode;

public class JwtAuthenticationException extends AuthenticationException {

    private final ErrorCode errorCode;

    public JwtAuthenticationException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
