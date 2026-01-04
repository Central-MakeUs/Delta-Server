package cmc.delta.global.config.security.jwt;

import cmc.delta.domain.auth.application.port.TokenIssuer;
import cmc.delta.global.config.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** JwtTokenProvider를 이용해 Access/Refresh 토큰 세트를 발급한다. */
@Component
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuer {

	private static final String TOKEN_TYPE_BEARER = "Bearer";

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public IssuedTokens issue(UserPrincipal principal) {
		String access = jwtTokenProvider.issueAccessToken(principal);
		String refresh = jwtTokenProvider.issueRefreshToken(principal);
		return new IssuedTokens(access, refresh, TOKEN_TYPE_BEARER);
	}
}
