package cmc.delta.domain.problem.infrastructure.ocr.mathpix;

import cmc.delta.domain.problem.infrastructure.support.ExternalCallFailureData;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.springframework.web.client.RestClientResponseException;

public class MathpixOcrException extends BusinessException {

	public static final String PROVIDER = "MATHPIX";

	public static final String REASON_EXTERNAL_CALL_FAILED = "MATHPIX_EXTERNAL_CALL_FAILED";
	public static final String REASON_EMPTY_RESPONSE_TEXT = "MATHPIX_EMPTY_RESPONSE_TEXT";
	public static final String REASON_OPTIONS_JSON_BUILD_FAILED = "MATHPIX_OPTIONS_JSON_BUILD_FAILED";
	public static final String REASON_RESPONSE_PARSE_FAILED = "MATHPIX_RESPONSE_PARSE_FAILED";

	private MathpixOcrException(ErrorCode code, String message, Object data, Throwable cause) {
		super(code, message, data);
		if (cause != null) {
			initCause(cause);
		}
	}

	public static MathpixOcrException externalCallFailed(RestClientResponseException cause) {
		ExternalCallFailureData data = new ExternalCallFailureData(PROVIDER, REASON_EXTERNAL_CALL_FAILED, cause.getRawStatusCode());
		return new MathpixOcrException(ErrorCode.OCR_PROCESSING_FAILED, REASON_EXTERNAL_CALL_FAILED, data, cause);
	}

	public static MathpixOcrException optionsJsonBuildFailed(Throwable cause) {
		ExternalCallFailureData data = new ExternalCallFailureData(PROVIDER, REASON_OPTIONS_JSON_BUILD_FAILED, null);
		return new MathpixOcrException(ErrorCode.INTERNAL_ERROR, REASON_OPTIONS_JSON_BUILD_FAILED, data, cause);
	}

	public static MathpixOcrException responseParseFailed(Throwable cause) {
		ExternalCallFailureData data = new ExternalCallFailureData(PROVIDER, REASON_RESPONSE_PARSE_FAILED, null);
		return new MathpixOcrException(ErrorCode.OCR_PROCESSING_FAILED, REASON_RESPONSE_PARSE_FAILED, data, cause);
	}

	public static MathpixOcrException emptyResponseText() {
		ExternalCallFailureData data = new ExternalCallFailureData(PROVIDER, REASON_EMPTY_RESPONSE_TEXT, null);
		return new MathpixOcrException(ErrorCode.OCR_PROCESSING_FAILED, REASON_EMPTY_RESPONSE_TEXT, data, null);
	}
}
