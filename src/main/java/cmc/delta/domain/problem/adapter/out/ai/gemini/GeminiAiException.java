package cmc.delta.domain.problem.adapter.out.ai.gemini;

import cmc.delta.domain.problem.adapter.out.support.ExternalCallFailureData;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.springframework.web.client.RestClientResponseException;

public class GeminiAiException extends BusinessException {

	public static final String PROVIDER = "GEMINI";

	public static final String REASON_EXTERNAL_CALL_FAILED = "GEMINI_EXTERNAL_CALL_FAILED";
	public static final String REASON_EMPTY_TEXT = "GEMINI_EMPTY_TEXT";
	public static final String REASON_PROMPT_BUILD_FAILED = "GEMINI_PROMPT_BUILD_FAILED";
	public static final String REASON_RESPONSE_PARSE_FAILED = "GEMINI_RESPONSE_PARSE_FAILED";

	private GeminiAiException(ErrorCode code, String message, Object data, Throwable cause) {
		super(code, message, data);
		if (cause != null) {
			initCause(cause);
		}
	}

	public static GeminiAiException externalCallFailed(RestClientResponseException cause) {
		ExternalCallFailureData data = new ExternalCallFailureData(PROVIDER, REASON_EXTERNAL_CALL_FAILED,
			cause.getRawStatusCode());
		return new GeminiAiException(ErrorCode.AI_PROCESSING_FAILED, REASON_EXTERNAL_CALL_FAILED, data, cause);
	}

	public static GeminiAiException emptyText() {
		ExternalCallFailureData data = new ExternalCallFailureData(PROVIDER, REASON_EMPTY_TEXT, null);
		return new GeminiAiException(ErrorCode.AI_PROCESSING_FAILED, REASON_EMPTY_TEXT, data, null);
	}

	public static GeminiAiException promptBuildFailed(Throwable cause) {
		ExternalCallFailureData data = new ExternalCallFailureData(PROVIDER, REASON_PROMPT_BUILD_FAILED, null);
		return new GeminiAiException(ErrorCode.INTERNAL_ERROR, REASON_PROMPT_BUILD_FAILED, data, cause);
	}

	public static GeminiAiException responseParseFailed(Throwable cause) {
		ExternalCallFailureData data = new ExternalCallFailureData(PROVIDER, REASON_RESPONSE_PARSE_FAILED, null);
		return new GeminiAiException(ErrorCode.AI_PROCESSING_FAILED, REASON_RESPONSE_PARSE_FAILED, data, cause);
	}
}
