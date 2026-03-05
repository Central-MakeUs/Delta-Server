package cmc.delta.domain.problem.adapter.out.ai.openai;

import cmc.delta.domain.problem.adapter.out.support.ExternalCallFailureData;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.springframework.web.client.RestClientResponseException;

public class OpenAiAiException extends BusinessException {

	public static final String PROVIDER = "OPENAI";

	public static final String REASON_EXTERNAL_CALL_FAILED = "OPENAI_EXTERNAL_CALL_FAILED";
	public static final String REASON_EMPTY_TEXT = "OPENAI_EMPTY_TEXT";
	public static final String REASON_PROMPT_BUILD_FAILED = "OPENAI_PROMPT_BUILD_FAILED";
	public static final String REASON_RESPONSE_PARSE_FAILED = "OPENAI_RESPONSE_PARSE_FAILED";

	private OpenAiAiException(ErrorCode code, String message, Object data, Throwable cause) {
		super(code, message, data);
		if (cause != null) {
			initCause(cause);
		}
	}

	public static OpenAiAiException externalCallFailed(RestClientResponseException cause) {
		ExternalCallFailureData data = new ExternalCallFailureData(PROVIDER, REASON_EXTERNAL_CALL_FAILED,
			cause.getRawStatusCode());
		return new OpenAiAiException(ErrorCode.AI_PROCESSING_FAILED, REASON_EXTERNAL_CALL_FAILED, data, cause);
	}

	public static OpenAiAiException emptyText() {
		ExternalCallFailureData data = new ExternalCallFailureData(PROVIDER, REASON_EMPTY_TEXT, null);
		return new OpenAiAiException(ErrorCode.AI_PROCESSING_FAILED, REASON_EMPTY_TEXT, data, null);
	}

	public static OpenAiAiException promptBuildFailed(Throwable cause) {
		ExternalCallFailureData data = new ExternalCallFailureData(PROVIDER, REASON_PROMPT_BUILD_FAILED, null);
		return new OpenAiAiException(ErrorCode.INTERNAL_ERROR, REASON_PROMPT_BUILD_FAILED, data, cause);
	}

	public static OpenAiAiException responseParseFailed(Throwable cause) {
		ExternalCallFailureData data = new ExternalCallFailureData(PROVIDER, REASON_RESPONSE_PARSE_FAILED, null);
		return new OpenAiAiException(ErrorCode.AI_PROCESSING_FAILED, REASON_RESPONSE_PARSE_FAILED, data, cause);
	}
}
