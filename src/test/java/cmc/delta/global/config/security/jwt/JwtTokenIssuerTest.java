package cmc.delta.global.config.security.jwt;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.global.config.security.principal.UserPrincipal;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenIssuerTest {

	private JwtTokenIssuer tokenIssuer;

	@BeforeEach
	void setUp() {
		JwtProperties properties = new JwtProperties(
			"delta-test-issuer",
			"dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2",
			Duration.ofMinutes(15).toSeconds(),
			Duration.ofDays(14).toSeconds(),
			new JwtProperties.Blacklist(true));

		JwtTokenProvider provider = new JwtTokenProvider(properties);
		tokenIssuer = new JwtTokenIssuer(provider);
	}

	@Test
	@DisplayName("parseAccessTokenInfo는 jti와 remainingTtl을 한 번에 반환함")
	void parseAccessTokenInfo_returnsBothJtiAndTtl() {
		// given
		UserPrincipal principal = new UserPrincipal(1L, "USER");
		TokenIssuer.IssuedTokens tokens = tokenIssuer.issue(principal);

		// when
		TokenIssuer.AccessTokenInfo info = tokenIssuer.parseAccessTokenInfo(tokens.accessToken());

		// then
		assertThat(info.jti()).isNotBlank();
		assertThat(info.remainingTtl()).isPositive();
		assertThat(info.remainingTtl()).isLessThanOrEqualTo(Duration.ofMinutes(15));
	}

	@Test
	@DisplayName("parseAccessTokenInfo의 remainingTtl은 만료 전이면 양수임")
	void parseAccessTokenInfo_whenNotExpired_thenTtlIsPositive() {
		// given
		UserPrincipal principal = new UserPrincipal(2L, "USER");
		TokenIssuer.IssuedTokens tokens = tokenIssuer.issue(principal);

		// when
		TokenIssuer.AccessTokenInfo info = tokenIssuer.parseAccessTokenInfo(tokens.accessToken());

		// then
		assertThat(info.remainingTtl().isZero()).isFalse();
		assertThat(info.remainingTtl().isNegative()).isFalse();
	}

	@Test
	@DisplayName("같은 토큰으로 두 번 parseAccessTokenInfo를 호출하면 동일한 jti를 반환함")
	void parseAccessTokenInfo_calledTwice_returnsSameJti() {
		// given
		UserPrincipal principal = new UserPrincipal(3L, "USER");
		TokenIssuer.IssuedTokens tokens = tokenIssuer.issue(principal);

		// when
		TokenIssuer.AccessTokenInfo first = tokenIssuer.parseAccessTokenInfo(tokens.accessToken());
		TokenIssuer.AccessTokenInfo second = tokenIssuer.parseAccessTokenInfo(tokens.accessToken());

		// then
		assertThat(first.jti()).isEqualTo(second.jti());
	}
}
