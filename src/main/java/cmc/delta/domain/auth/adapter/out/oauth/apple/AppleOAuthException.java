package cmc.delta.domain.auth.adapter.out.oauth.apple;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class AppleOAuthException extends BusinessException {

	private AppleOAuthException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message);
		if (cause != null) {
			initCause(cause);
		}
	}

	public static AppleOAuthException idTokenEmpty() {
		return new AppleOAuthException(ErrorCode.INVALID_REQUEST, "애플 id_token이 비어있습니다.", null);
	}

	public static AppleOAuthException authorizationCodeEmpty() {
		return new AppleOAuthException(ErrorCode.INVALID_REQUEST, "애플 authorization code가 비어있습니다.", null);
	}

	public static AppleOAuthException userJsonParseFailed(Throwable cause) {
		return new AppleOAuthException(ErrorCode.INVALID_REQUEST, "애플 user 파싱에 실패했습니다.", cause);
	}

	public static AppleOAuthException subEmpty() {
		return new AppleOAuthException(ErrorCode.AUTHENTICATION_FAILED, "애플 sub가 비어있습니다.", null);
	}

	public static AppleOAuthException idTokenSubEmpty() {
		return new AppleOAuthException(ErrorCode.AUTHENTICATION_FAILED, "애플 id_token(sub)이 비어있습니다.", null);
	}

	public static AppleOAuthException idTokenParseFailed(Throwable cause) {
		return new AppleOAuthException(ErrorCode.AUTHENTICATION_FAILED, "애플 id_token 파싱에 실패했습니다.", cause);
	}

	public static AppleOAuthException claimReadFailed(Throwable cause) {
		return new AppleOAuthException(ErrorCode.AUTHENTICATION_FAILED, "애플 클레임 조회에 실패했습니다.", cause);
	}

	public static AppleOAuthException issuerInvalid() {
		return new AppleOAuthException(ErrorCode.AUTHENTICATION_FAILED, "애플 iss가 올바르지 않습니다.", null);
	}

	public static AppleOAuthException audienceInvalid() {
		return new AppleOAuthException(ErrorCode.AUTHENTICATION_FAILED, "애플 aud가 올바르지 않습니다.", null);
	}

	public static AppleOAuthException tokenExpired() {
		return new AppleOAuthException(ErrorCode.AUTHENTICATION_FAILED, "애플 id_token이 만료되었습니다.", null);
	}

	public static AppleOAuthException kidEmpty() {
		return new AppleOAuthException(ErrorCode.AUTHENTICATION_FAILED, "애플 토큰 헤더 kid가 비어있습니다.", null);
	}

	public static AppleOAuthException publicKeyNotFound() {
		return new AppleOAuthException(
			ErrorCode.AUTHENTICATION_FAILED,
			"애플 공개키(kid)에 해당하는 키를 찾지 못했습니다.",
			null);
	}

	public static AppleOAuthException publicKeyTypeNotRsa(String keyType) {
		return new AppleOAuthException(
			ErrorCode.AUTHENTICATION_FAILED,
			"애플 공개키 타입이 RSA가 아닙니다: " + keyType,
			null);
	}

	public static AppleOAuthException algorithmNotRs256() {
		return new AppleOAuthException(
			ErrorCode.AUTHENTICATION_FAILED,
			"애플 토큰 알고리즘이 RS256이 아닙니다.",
			null);
	}

	public static AppleOAuthException signatureVerifyFailed() {
		return new AppleOAuthException(
			ErrorCode.AUTHENTICATION_FAILED,
			"애플 id_token 서명 검증에 실패했습니다.",
			null);
	}

	public static AppleOAuthException verifyUnexpectedError(Throwable cause) {
		return new AppleOAuthException(
			ErrorCode.AUTHENTICATION_FAILED,
			"애플 id_token 검증 중 오류가 발생했습니다.",
			cause);
	}

	public static AppleOAuthException jwkLoadFailed(Throwable cause) {
		return new AppleOAuthException(ErrorCode.OAUTH_JWK_LOAD_FAILED, "애플 공개키(JWK) 로딩에 실패했습니다.", cause);
	}

	public static AppleOAuthException tokenExchangeInvalidResponse() {
		return new AppleOAuthException(ErrorCode.OAUTH_TOKEN_EXCHANGE_INVALID_RESPONSE, "애플 토큰 교환 응답이 비어있습니다.", null);
	}

	public static AppleOAuthException tokenExchangeFailed(int status, Throwable cause) {
		return new AppleOAuthException(
			ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED,
			"애플 토큰 교환 실패 (status=" + status + ")",
			cause);
	}

	public static AppleOAuthException tokenExchangeTimeout(Throwable cause) {
		return new AppleOAuthException(ErrorCode.OAUTH_TOKEN_EXCHANGE_TIMEOUT, "애플 토큰 교환 타임아웃", cause);
	}

	public static AppleOAuthException clientSecretGenerateFailed(Throwable cause) {
		String causeName = (cause == null) ? "UNKNOWN" : cause.getClass().getSimpleName();
		return new AppleOAuthException(
			ErrorCode.INTERNAL_ERROR,
			"애플 client_secret 생성 실패: " + causeName,
			cause);
	}
}
