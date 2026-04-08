package cmc.delta.domain.problem.adapter.out.ai.openai;

import cmc.delta.domain.problem.adapter.out.ai.AbstractAiException;
import cmc.delta.global.error.ErrorCode;
import org.springframework.web.client.RestClientResponseException;

public class OpenAiAiException extends AbstractAiException {

	public static final String PROVIDER = "OPENAI";

	public static final String REASON_EXTERNAL_CALL_FAILED = "OPENAI_EXTERNAL_CALL_FAILED";
	public static final String REASON_EMPTY_TEXT = "OPENAI_EMPTY_TEXT";
	public static final String REASON_PROMPT_BUILD_FAILED = "OPENAI_PROMPT_BUILD_FAILED";
	public static final String REASON_RESPONSE_PARSE_FAILED = "OPENAI_RESPONSE_PARSE_FAILED";

	private OpenAiAiException(ErrorCode code, String message, Object data, Throwable cause) {
		super(code, message, data, cause);
	}

	public static OpenAiAiException externalCallFailed(RestClientResponseException cause) {
		return new OpenAiAiException(ErrorCode.AI_PROCESSING_FAILED, REASON_EXTERNAL_CALL_FAILED,
			externalCallData(PROVIDER, REASON_EXTERNAL_CALL_FAILED, cause), cause);
	}

	public static OpenAiAiException emptyText() {
		return new OpenAiAiException(ErrorCode.AI_PROCESSING_FAILED, REASON_EMPTY_TEXT,
			failureData(PROVIDER, REASON_EMPTY_TEXT), null);
	}

	public static OpenAiAiException promptBuildFailed(Throwable cause) {
		return new OpenAiAiException(ErrorCode.INTERNAL_ERROR, REASON_PROMPT_BUILD_FAILED,
			failureData(PROVIDER, REASON_PROMPT_BUILD_FAILED), cause);
	}

	public static OpenAiAiException responseParseFailed(Throwable cause) {
		return new OpenAiAiException(ErrorCode.AI_PROCESSING_FAILED, REASON_RESPONSE_PARSE_FAILED,
			failureData(PROVIDER, REASON_RESPONSE_PARSE_FAILED), cause);
	}
}
