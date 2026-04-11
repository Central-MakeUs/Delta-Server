package cmc.delta.domain.problem.adapter.out.ai.gemini;

import cmc.delta.domain.problem.adapter.out.ai.AbstractAiException;
import cmc.delta.global.error.ErrorCode;
import org.springframework.web.client.RestClientResponseException;

public class GeminiAiException extends AbstractAiException {

	public static final String PROVIDER = "GEMINI";

	public static final String REASON_EXTERNAL_CALL_FAILED = "GEMINI_EXTERNAL_CALL_FAILED";
	public static final String REASON_EMPTY_TEXT = "GEMINI_EMPTY_TEXT";
	public static final String REASON_PROMPT_BUILD_FAILED = "GEMINI_PROMPT_BUILD_FAILED";
	public static final String REASON_RESPONSE_PARSE_FAILED = "GEMINI_RESPONSE_PARSE_FAILED";

	private GeminiAiException(ErrorCode code, String message, Object data, Throwable cause) {
		super(code, message, data, cause);
	}

	public static GeminiAiException externalCallFailed(RestClientResponseException cause) {
		return new GeminiAiException(ErrorCode.AI_PROCESSING_FAILED, REASON_EXTERNAL_CALL_FAILED,
			externalCallData(PROVIDER, REASON_EXTERNAL_CALL_FAILED, cause), cause);
	}

	public static GeminiAiException emptyText() {
		return new GeminiAiException(ErrorCode.AI_PROCESSING_FAILED, REASON_EMPTY_TEXT,
			failureData(PROVIDER, REASON_EMPTY_TEXT), null);
	}

	public static GeminiAiException promptBuildFailed(Throwable cause) {
		return new GeminiAiException(ErrorCode.INTERNAL_ERROR, REASON_PROMPT_BUILD_FAILED,
			failureData(PROVIDER, REASON_PROMPT_BUILD_FAILED), cause);
	}

	public static GeminiAiException responseParseFailed(Throwable cause) {
		return new GeminiAiException(ErrorCode.AI_PROCESSING_FAILED, REASON_RESPONSE_PARSE_FAILED,
			failureData(PROVIDER, REASON_RESPONSE_PARSE_FAILED), cause);
	}

	public boolean isResponseParseFailure() {
		return isResponseParseFailure(REASON_RESPONSE_PARSE_FAILED);
	}
}
