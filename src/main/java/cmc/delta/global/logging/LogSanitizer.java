package cmc.delta.global.logging;

public final class LogSanitizer {

	private LogSanitizer() {}

	/** 로그 위조(CRLF 인젝션) 방지를 위해 개행을 제거하고 최대 길이로 자른다. */
	public static String sanitize(String raw, int maxLength) {
		if (raw == null) {
			return LoggingConstants.Header.UNKNOWN;
		}
		String compact = raw.replace("\n", " ").replace("\r", " ").trim();
		if (compact.length() > maxLength) {
			return compact.substring(0, maxLength);
		}
		return compact;
	}
}
