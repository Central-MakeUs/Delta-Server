package cmc.delta.domain.auth.application.token;

public final class AuthHeaderConstants {

	private AuthHeaderConstants() {}

	public static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
	public static final String EXPOSE_HEADERS_VALUE = "Authorization, X-Refresh-Token, X-Trace-Id";
}
