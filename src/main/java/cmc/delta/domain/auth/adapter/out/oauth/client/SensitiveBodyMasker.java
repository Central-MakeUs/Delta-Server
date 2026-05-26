package cmc.delta.domain.auth.adapter.out.oauth.client;

import java.util.regex.Pattern;

/**
 * OAuth provider 응답 body 에서 토큰/인가코드 등 민감 필드를 "***" 로 치환한다.
 * 실패 로그에 body 를 남길 때만 사용 — 정상 응답은 절대 로깅하지 않는다.
 */
final class SensitiveBodyMasker {

	private static final String MASKED = "\"***\"";

	private static final Pattern SENSITIVE_FIELD = Pattern.compile(
		"(\"(?:access_token|refresh_token|id_token|code|client_secret|authorization_code)\"\\s*:\\s*)\"[^\"]*\"");

	private SensitiveBodyMasker() {}

	static String mask(String body) {
		if (body == null || body.isBlank()) {
			return "";
		}
		return SENSITIVE_FIELD.matcher(body).replaceAll("$1" + MASKED);
	}
}
