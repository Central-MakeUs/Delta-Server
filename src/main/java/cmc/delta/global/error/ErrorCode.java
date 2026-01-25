package cmc.delta.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	// AUTH
	AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증에 실패했습니다.", LogLevel.WARN),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다.", LogLevel.WARN),

	// OAUTH
	OAUTH_PROVIDER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_100", "외부 OAuth 호출에 실패했습니다.", LogLevel.ERROR),
	OAUTH_PROVIDER_TIMEOUT(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_101", "외부 OAuth 호출이 지연되고 있습니다.", LogLevel.ERROR),
	OAUTH_INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_102", "외부 OAuth 응답이 올바르지 않습니다.", LogLevel.ERROR),

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
	USER_WITHDRAWN(HttpStatus.FORBIDDEN, "USER_002", "탈퇴한 사용자입니다.", LogLevel.WARN),
	USER_ONBOARDING_REQUIRED(HttpStatus.FORBIDDEN, "USER_003", "추가 정보 입력이 필요합니다.", LogLevel.WARN),

	// PROBLEM
	PROBLEM_SCAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PROB_001", "스캔을 찾을 수 없습니다.", LogLevel.WARN),
	PROBLEM_ASSET_NOT_FOUND(HttpStatus.NOT_FOUND, "PROB_002", "원본 이미지를 찾을 수 없습니다.", LogLevel.WARN),
	PROBLEM_SCAN_FORBIDDEN(HttpStatus.FORBIDDEN, "PROB_003", "해당 스캔에 접근할 수 없습니다.", LogLevel.WARN),
	PROBLEM_NOT_FOUND(HttpStatus.NOT_FOUND, "PROB_200", "오답카드를 찾을 수 없습니다.", LogLevel.WARN),
	PROBLEM_UPDATE_EMPTY(HttpStatus.BAD_REQUEST, "PROB_201", "수정할 값이 없습니다.", LogLevel.WARN),
	PROBLEM_UPDATE_INVALID_ANSWER(HttpStatus.BAD_REQUEST, "PROB_202", "정답 입력값이 올바르지 않습니다.", LogLevel.WARN),

	// 오답카드 생성/저장
	PROBLEM_FINAL_UNIT_MUST_BE_CHILD(HttpStatus.BAD_REQUEST, "PROB_004", "단원은 과목이 아닌 하위 단원을 선택해야 합니다.", LogLevel.WARN),
	PROBLEM_FINAL_UNIT_NOT_FOUND(HttpStatus.BAD_REQUEST, "PROB_005", "선택한 단원을 찾을 수 없습니다.", LogLevel.WARN),
	PROBLEM_FINAL_TYPE_NOT_FOUND(HttpStatus.BAD_REQUEST, "PROB_006", "선택한 유형을 찾을 수 없습니다.", LogLevel.WARN),
	PROBLEM_SCAN_NOT_READY(HttpStatus.BAD_REQUEST, "PROB_007", "AI 분석이 완료된 스캔만 오답카드를 생성할 수 있습니다.", LogLevel.WARN),
	PROBLEM_ALREADY_CREATED(HttpStatus.CONFLICT, "PROB_008", "이미 해당 스캔으로 생성된 오답카드가 있습니다.", LogLevel.WARN),
	PROBLEM_SCAN_RENDER_MODE_MISSING(HttpStatus.BAD_REQUEST, "PROB_009", "스캔 렌더 모드가 누락되었습니다.", LogLevel.WARN),
	PROBLEM_LIST_INVALID_PAGINATION(HttpStatus.BAD_REQUEST, "PROB_100", "페이지네이션 값이 올바르지 않습니다.", LogLevel.WARN),

	// OCR / AI
	OCR_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROB_010", "OCR 처리에 실패했습니다.", LogLevel.ERROR),
	AI_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PROB_020", "AI 처리에 실패했습니다.", LogLevel.ERROR);

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
