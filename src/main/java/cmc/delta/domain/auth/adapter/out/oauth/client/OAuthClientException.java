package cmc.delta.domain.auth.adapter.out.oauth.client;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class OAuthClientException extends BusinessException {

	public static final String OP_TOKEN_EXCHANGE = "토큰 교환";
	public static final String OP_PROFILE_FETCH = "유저 조회";

	private OAuthClientException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message);
		if (cause != null) {
			initCause(cause);
		}
	}

	public static OAuthClientException tokenExchangeError(
		String providerName, int status, Throwable cause) {
		return new OAuthClientException(
			ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED,
			providerName + " " + OP_TOKEN_EXCHANGE + " 실패 (status=" + status + ")",
			cause);
	}

	public static OAuthClientException tokenExchangeTimeout(String providerName, Throwable cause) {
		return new OAuthClientException(
			ErrorCode.OAUTH_TOKEN_EXCHANGE_TIMEOUT,
			providerName + " " + OP_TOKEN_EXCHANGE + " 타임아웃",
			cause);
	}

	public static OAuthClientException tokenExchangeInvalidResponse(String providerName) {
		return new OAuthClientException(
			ErrorCode.OAUTH_TOKEN_EXCHANGE_INVALID_RESPONSE,
			providerName + " " + OP_TOKEN_EXCHANGE + " 응답이 비어있습니다.",
			null);
	}

	public static OAuthClientException profileFetchError(
		String providerName, int status, Throwable cause) {
		return new OAuthClientException(
			ErrorCode.OAUTH_PROFILE_FETCH_FAILED,
			providerName + " " + OP_PROFILE_FETCH + " 실패 (status=" + status + ")",
			cause);
	}

	public static OAuthClientException profileFetchTimeout(String providerName, Throwable cause) {
		return new OAuthClientException(
			ErrorCode.OAUTH_PROFILE_FETCH_TIMEOUT,
			providerName + " " + OP_PROFILE_FETCH + " 타임아웃",
			cause);
	}

	public static OAuthClientException profileFetchInvalidResponse(String providerName) {
		return new OAuthClientException(
			ErrorCode.OAUTH_PROFILE_FETCH_INVALID_RESPONSE,
			providerName + " " + OP_PROFILE_FETCH + " 응답이 비어있습니다.",
			null);
	}

	public static OAuthClientException providerErrorFallback(
		String providerName, String operation, int status, Throwable cause) {
		return new OAuthClientException(
			ErrorCode.OAUTH_PROVIDER_ERROR,
			providerName + " " + operation + " 실패 (status=" + status + ")",
			cause);
	}

	public static OAuthClientException providerTimeoutFallback(
		String providerName, String operation, Throwable cause) {
		return new OAuthClientException(
			ErrorCode.OAUTH_PROVIDER_TIMEOUT,
			providerName + " " + operation + " 타임아웃",
			cause);
	}

	public static OAuthClientException invalidResponseFallback(String providerName, String operation) {
		return new OAuthClientException(
			ErrorCode.OAUTH_INVALID_RESPONSE,
			providerName + " " + operation + " 응답이 비어있습니다.",
			null);
	}
}
