package cmc.delta.domain.problem.adapter.out.ai;

import cmc.delta.domain.problem.adapter.out.support.ExternalCallFailureData;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

public abstract class AbstractAiException extends BusinessException {

	protected AbstractAiException(ErrorCode code, String message, Object data, Throwable cause) {
		super(code, message, data);
		if (cause != null) {
			initCause(cause);
		}
	}

	protected static ExternalCallFailureData externalCallData(String provider, String reason,
		RestClientResponseException cause) {
		return new ExternalCallFailureData(provider, reason, cause.getRawStatusCode());
	}

	protected static ExternalCallFailureData failureData(String provider, String reason) {
		return new ExternalCallFailureData(provider, reason, null);
	}

	public boolean isRateLimited() {
		return httpStatus() != null && httpStatus() == HttpStatus.TOO_MANY_REQUESTS.value();
	}

	public boolean isFallbackEligibleStatus() {
		Integer status = httpStatus();
		if (status == null) {
			return false;
		}
		if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
			return true;
		}
		if (status == HttpStatus.REQUEST_TIMEOUT.value()) {
			return true;
		}
		return status >= HttpStatus.INTERNAL_SERVER_ERROR.value();
	}

	public boolean isResponseParseFailure(String reason) {
		return reason.equals(getMessage());
	}

	public Integer httpStatus() {
		Object data = getData();
		if (!(data instanceof ExternalCallFailureData externalCallFailureData)) {
			return null;
		}
		return externalCallFailureData.httpStatus();
	}
}
