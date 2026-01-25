package cmc.delta.domain.auth.adapter.out.oauth.client;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class OAuthClientException extends BusinessException {

	private OAuthClientException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message);
		if (cause != null) {
			initCause(cause);
		}
	}

	public static OAuthClientException providerError(
		String providerName, String operation, int status, Throwable cause) {
		return new OAuthClientException(
			ErrorCode.OAUTH_PROVIDER_ERROR,
			providerName + " " + operation + " 실패 (status=" + status + ")",
			cause);
	}

	public static OAuthClientException providerTimeout(
		String providerName, String operation, Throwable cause) {
		return new OAuthClientException(
			ErrorCode.OAUTH_PROVIDER_TIMEOUT,
			providerName + " " + operation + " 타임아웃",
			cause);
	}

	public static OAuthClientException invalidResponse(String providerName, String operation) {
		return new OAuthClientException(
			ErrorCode.OAUTH_INVALID_RESPONSE,
			providerName + " " + operation + " 응답이 비어있습니다.",
			null);
	}
}
