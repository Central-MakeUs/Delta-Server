package cmc.delta.domain.auth.adapter.in.support;

import static cmc.delta.domain.auth.adapter.in.support.AuthHeaderConstants.*;

import cmc.delta.domain.auth.application.exception.TokenException;
import cmc.delta.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HttpTokenExtractor {

	public String extractRefreshToken(HttpServletRequest request) {
		String refreshToken = request.getHeader(REFRESH_TOKEN_HEADER);
		if (!StringUtils.hasText(refreshToken)) {
			throw new TokenException(ErrorCode.REFRESH_TOKEN_REQUIRED);
		}
		return refreshToken.trim();
	}

	public String extractAccessToken(HttpServletRequest request) {
		String authorization = request.getHeader(AUTHORIZATION_HEADER);
		if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
			throw new TokenException(ErrorCode.TOKEN_REQUIRED);
		}
		return authorization.substring(BEARER_PREFIX.length()).trim();
	}
}
