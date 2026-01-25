package cmc.delta.global.config.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** Authorization 헤더에서 Bearer 토큰을 추출. */
@Component
public class BearerTokenResolver {

	private static final String HEADER_AUTHORIZATION = "Authorization";
	private static final String PREFIX = "Bearer ";

	public String resolveBearerToken(HttpServletRequest request) {
		String raw = request.getHeader(HEADER_AUTHORIZATION);
		if (raw == null || raw.isBlank() || !raw.startsWith(PREFIX)) {
			return null;
		}
		String token = raw.substring(PREFIX.length()).trim();
		return token.isBlank() ? null : token;
	}
}
