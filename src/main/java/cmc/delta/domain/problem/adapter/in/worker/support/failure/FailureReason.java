package cmc.delta.domain.problem.adapter.in.worker.support.failure;

public enum FailureReason {

	SCAN_NOT_FOUND("SCAN_NOT_FOUND"),
	ASSET_NOT_FOUND("ASSET_NOT_FOUND"),
	OCR_TEXT_EMPTY("OCR_TEXT_EMPTY"),
	OCR_NOT_MATH("OCR_NOT_MATH"),

	OCR_RATE_LIMIT("OCR_RATE_LIMIT"),
	OCR_CLIENT_4XX("OCR_CLIENT_4XX"),
	OCR_CLIENT_5XX("OCR_CLIENT_5XX"),
	OCR_NETWORK_ERROR("OCR_NETWORK_ERROR"),
	OCR_FAILED("OCR_FAILED"),

	AI_RATE_LIMIT("AI_RATE_LIMIT"),
	AI_NOT_MATH("AI_NOT_MATH"),
	AI_CLIENT_4XX("AI_CLIENT_4XX"),
	AI_CLIENT_5XX("AI_CLIENT_5XX"),
	AI_NETWORK_ERROR("AI_NETWORK_ERROR"),
	AI_FAILED("AI_FAILED");

	private final String code;

	FailureReason(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}
}
