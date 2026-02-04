package cmc.delta.domain.auth.adapter.in.support;

public final class AuthHeaderConstants {

	private AuthHeaderConstants() {}

	public static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
	public static final String EXPOSE_HEADERS_VALUE = "Authorization, X-Refresh-Token, X-Trace-Id";

	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BEARER_PREFIX = "Bearer ";
}
