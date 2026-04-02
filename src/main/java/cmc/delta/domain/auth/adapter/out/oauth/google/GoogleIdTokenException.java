package cmc.delta.domain.auth.adapter.out.oauth.google;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class GoogleIdTokenException extends BusinessException {

	private GoogleIdTokenException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message);
		if (cause != null) {
			initCause(cause);
		}
	}

	public static GoogleIdTokenException idTokenEmpty() {
		return new GoogleIdTokenException(ErrorCode.INVALID_REQUEST, "구글 id_token이 비어있습니다.", null);
	}

	public static GoogleIdTokenException subEmpty() {
		return new GoogleIdTokenException(ErrorCode.AUTHENTICATION_FAILED, "구글 sub가 비어있습니다.", null);
	}

	public static GoogleIdTokenException idTokenParseFailed(Throwable cause) {
		return new GoogleIdTokenException(ErrorCode.AUTHENTICATION_FAILED, "구글 id_token 파싱에 실패했습니다.", cause);
	}

	public static GoogleIdTokenException claimReadFailed(Throwable cause) {
		return new GoogleIdTokenException(ErrorCode.AUTHENTICATION_FAILED, "구글 클레임 조회에 실패했습니다.", cause);
	}

	public static GoogleIdTokenException issuerInvalid() {
		return new GoogleIdTokenException(ErrorCode.AUTHENTICATION_FAILED, "구글 iss가 올바르지 않습니다.", null);
	}

	public static GoogleIdTokenException audienceInvalid() {
		return new GoogleIdTokenException(ErrorCode.AUTHENTICATION_FAILED, "구글 aud가 올바르지 않습니다.", null);
	}

	public static GoogleIdTokenException tokenExpired() {
		return new GoogleIdTokenException(ErrorCode.AUTHENTICATION_FAILED, "구글 id_token이 만료되었습니다.", null);
	}

	public static GoogleIdTokenException kidEmpty() {
		return new GoogleIdTokenException(ErrorCode.AUTHENTICATION_FAILED, "구글 토큰 헤더 kid가 비어있습니다.", null);
	}

	public static GoogleIdTokenException publicKeyNotFound() {
		return new GoogleIdTokenException(
			ErrorCode.AUTHENTICATION_FAILED,
			"구글 공개키(kid)에 해당하는 키를 찾지 못했습니다.",
			null);
	}

	public static GoogleIdTokenException publicKeyTypeNotRsa(String keyType) {
		return new GoogleIdTokenException(
			ErrorCode.AUTHENTICATION_FAILED,
			"구글 공개키 타입이 RSA가 아닙니다: " + keyType,
			null);
	}

	public static GoogleIdTokenException algorithmNotRs256() {
		return new GoogleIdTokenException(
			ErrorCode.AUTHENTICATION_FAILED,
			"구글 토큰 알고리즘이 RS256이 아닙니다.",
			null);
	}

	public static GoogleIdTokenException signatureVerifyFailed() {
		return new GoogleIdTokenException(
			ErrorCode.AUTHENTICATION_FAILED,
			"구글 id_token 서명 검증에 실패했습니다.",
			null);
	}

	public static GoogleIdTokenException verifyUnexpectedError(Throwable cause) {
		return new GoogleIdTokenException(
			ErrorCode.AUTHENTICATION_FAILED,
			"구글 id_token 검증 중 오류가 발생했습니다.",
			cause);
	}

	public static GoogleIdTokenException jwkLoadFailed(Throwable cause) {
		return new GoogleIdTokenException(ErrorCode.OAUTH_JWK_LOAD_FAILED, "구글 공개키(JWK) 로딩에 실패했습니다.", cause);
	}
}
