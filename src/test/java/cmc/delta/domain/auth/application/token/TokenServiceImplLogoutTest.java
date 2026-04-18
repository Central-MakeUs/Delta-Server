package cmc.delta.domain.auth.application.token;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.auth.application.exception.TokenException;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.auth.application.service.token.TokenServiceImpl;
import cmc.delta.domain.auth.application.support.FakeTokenIssuer;
import cmc.delta.domain.auth.application.support.InMemoryAccessBlacklistStore;
import cmc.delta.domain.auth.application.support.InMemoryRefreshTokenStore;
import cmc.delta.domain.auth.application.support.RefreshTokenHasher;
import cmc.delta.domain.auth.application.support.TokenFixtures;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenServiceImplLogoutTest {

	private InMemoryRefreshTokenStore refreshTokenStore;
	private InMemoryAccessBlacklistStore blacklistStore;
	private FakeTokenIssuer tokenIssuer;
	private TokenServiceImpl tokenService;

	@BeforeEach
	void setUp() {
		refreshTokenStore = new InMemoryRefreshTokenStore(TokenFixtures.fixedClock());
		blacklistStore = new InMemoryAccessBlacklistStore();
		tokenIssuer = new FakeTokenIssuer(TokenFixtures.fixedClock(), Duration.ofMinutes(15));

		tokenService = new TokenServiceImpl(
			tokenIssuer,
			refreshTokenStore,
			blacklistStore,
			TokenFixtures.noopAuditLogger());
	}

	@Test
	@DisplayName("로그아웃하면 리프레시 해시가 삭제됨")
	void logout_whenCalled_thenRefreshIsDeleted() {
		// given
		UserPrincipal principal = TokenFixtures.principal(1L);
		TokenIssuer.IssuedTokens tokens = tokenService.issue(principal);

		// when
		tokenService.logout(principal.userId(), tokens.accessToken(), tokens.refreshToken());

		// then
		assertThat(refreshTokenStore.getSavedHashOrNull(principal.userId(), TokenFixtures.DEFAULT_SESSION_ID)).isNull();
	}

	@Test
	@DisplayName("로그아웃하면 액세스 jti가 블랙리스트에 등록됨")
	void logout_whenCalled_thenAccessJtiIsBlacklisted() {
		// given
		UserPrincipal principal = TokenFixtures.principal(1L);
		TokenIssuer.IssuedTokens tokens = tokenService.issue(principal);
		String jti = tokenIssuer.extractJtiFromAccessToken(tokens.accessToken());

		// when
		tokenService.logout(principal.userId(), tokens.accessToken(), tokens.refreshToken());

		// then
		assertThat(blacklistStore.isBlacklisted(jti)).isTrue();
	}

	@Test
	@DisplayName("로그아웃 시 리프레시 해시가 불일치하면 INVALID_REFRESH_TOKEN이 발생함")
	void logout_whenRefreshMismatch_thenThrowsInvalidRefreshToken() {
		// given
		UserPrincipal principal = TokenFixtures.principal(1L);
		TokenIssuer.IssuedTokens tokens = tokenService.issue(principal);
		String wrongRefresh = tokens.refreshToken() + "_tampered";

		// when
		TokenException ex = catchThrowableOfType(
			() -> tokenService.logout(principal.userId(), tokens.accessToken(), wrongRefresh),
			TokenException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("로그아웃 후 리프레시 해시 TTL이 갱신되지 않음 — rotate 오용 방지")
	void logout_doesNotRotateRefreshHash() {
		// given
		UserPrincipal principal = TokenFixtures.principal(1L);
		TokenIssuer.IssuedTokens tokens = tokenService.issue(principal);
		String hashBefore = refreshTokenStore.getSavedHashOrNull(principal.userId(), TokenFixtures.DEFAULT_SESSION_ID);

		// when
		tokenService.logout(principal.userId(), tokens.accessToken(), tokens.refreshToken());

		// then: 로그아웃 후 해시 자체는 삭제됨 (rotate 됐다면 새 해시로 바뀌어 있을 것)
		String hashAfter = refreshTokenStore.getSavedHashOrNull(principal.userId(), TokenFixtures.DEFAULT_SESSION_ID);
		assertThat(hashBefore).isEqualTo(RefreshTokenHasher.sha256(tokens.refreshToken()));
		assertThat(hashAfter).isNull();
	}
}
