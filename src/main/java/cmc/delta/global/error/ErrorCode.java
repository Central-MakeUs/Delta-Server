package cmc.delta.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	// AUTH
	AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증에 실패했습니다.", LogLevel.WARN),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다.", LogLevel.WARN),

	// JWT (Access)
	TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_010", "토큰이 필요합니다.", LogLevel.WARN), // access 누락
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_011", "유효하지 않은 토큰입니다.", LogLevel.WARN), // 서명/형식 오류
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_012", "만료된 토큰입니다.", LogLevel.WARN), // exp 만료
	BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_013", "폐기된 토큰입니다.", LogLevel.WARN), // (옵션) 블랙리스트

	// JWT (Refresh)
	REFRESH_TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_020", "리프레시 토큰이 필요합니다.", LogLevel.WARN),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_021", "유효하지 않은 리프레시 토큰입니다.", LogLevel.WARN),

	// REQUEST
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "REQ_001", "요청이 올바르지 않습니다.", LogLevel.WARN),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "REQ_002", "허용되지 않은 메서드입니다.", LogLevel.WARN),

	// RESOURCE
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RES_404", "리소스를 찾을 수 없습니다.", LogLevel.WARN),

	// SYSTEM
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_500", "서버 오류가 발생했습니다.", LogLevel.ERROR),

	// USER
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다.", LogLevel.WARN),
	USER_WITHDRAWN(HttpStatus.FORBIDDEN, "USER_002", "탈퇴한 사용자입니다.", LogLevel.WARN);


	private final HttpStatus status;
	private final String code;
	private final String defaultMessage;
	private final LogLevel logLevel;

	ErrorCode(HttpStatus status, String code, String defaultMessage, LogLevel logLevel) {
		this.status = status;
		this.code = code;
		this.defaultMessage = defaultMessage;
		this.logLevel = logLevel;
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	public String defaultMessage() {
		return defaultMessage;
	}

	public LogLevel logLevel() {
		return logLevel;
	}

	public enum LogLevel {
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR
	}
}
