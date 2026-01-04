package cmc.delta.domain.auth.application.token;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.auth.application.port.TokenIssuer;
import cmc.delta.domain.auth.application.support.FakeTokenIssuer;
import cmc.delta.domain.auth.application.support.InMemoryAccessBlacklistStore;
import cmc.delta.domain.auth.application.support.InMemoryRefreshTokenStore;
import cmc.delta.domain.auth.application.support.TokenFixtures;
import cmc.delta.domain.auth.application.token.exception.TokenException;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenServiceImplTest {

	private Clock clock;
	private InMemoryRefreshTokenStore refreshTokenStore;
	private InMemoryAccessBlacklistStore blacklistStore;
	private FakeTokenIssuer tokenIssuer;

	private TokenServiceImpl tokenService;

	@BeforeEach
	void setUp() {
		clock = TokenFixtures.fixedClock();
		refreshTokenStore = new InMemoryRefreshTokenStore(clock);
		blacklistStore = new InMemoryAccessBlacklistStore();
		tokenIssuer = new FakeTokenIssuer(clock, Duration.ofMinutes(15));

		tokenService = new TokenServiceImpl(
			tokenIssuer,
			refreshTokenStore,
			blacklistStore,
			TokenFixtures.noopAuditLogger()
		);
	}

	@Test
	@DisplayName("요청하면 리프레시 해시가 저장됨")
	void issue_whenCalled_thenRefreshHashIsSaved() {
		// given
		UserPrincipal principal = TokenFixtures.principal(1L);

		// when
		TokenIssuer.IssuedTokens tokens = tokenService.issue(principal);

		// then
		String savedHash = refreshTokenStore.getSavedHashOrNull(principal.userId(), TokenFixtures.DEFAULT_SESSION_ID);
		assertThat(savedHash).isEqualTo(RefreshTokenHasher.sha256(tokens.refreshToken()));
	}

	@Test
	@DisplayName("유효한 리프레시로 재발급하면 리프레시 해시가 회전됨")
	void reissue_whenValidRefresh_thenRefreshHashIsRotated() {
		// given
		UserPrincipal principal = TokenFixtures.principal(1L);
		TokenIssuer.IssuedTokens issued = tokenService.issue(principal);

		// when
		TokenIssuer.IssuedTokens reissued = tokenService.reissue(issued.refreshToken());

		// then
		String savedHash = refreshTokenStore.getSavedHashOrNull(principal.userId(), TokenFixtures.DEFAULT_SESSION_ID);
		assertThat(savedHash).isEqualTo(RefreshTokenHasher.sha256(reissued.refreshToken()));
	}

	@Test
	@DisplayName("이전 리프레시를 재사용하면 INVALID_REFRESH_TOKEN이 발생함")
	void reissue_whenOldRefreshReused_thenThrowsInvalidRefreshToken() {
		// given
		UserPrincipal principal = TokenFixtures.principal(1L);
		TokenIssuer.IssuedTokens issued = tokenService.issue(principal);
		tokenService.reissue(issued.refreshToken());

		// when
		TokenException ex = catchThrowableOfType(
			() -> tokenService.reissue(issued.refreshToken()),
			TokenException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("리프레시가 비어있으면 REFRESH_TOKEN_REQUIRED가 발생함")
	void reissue_whenBlankRefresh_thenThrowsRefreshTokenRequired() {
		// given
		String refreshToken = "   ";

		// when
		TokenException ex = catchThrowableOfType(
			() -> tokenService.reissue(refreshToken),
			TokenException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_REQUIRED);
	}

	@Test
	@DisplayName("무효화하면 저장된 리프레시가 삭제됨")
	void invalidateAll_whenCalled_thenRefreshIsDeleted() {
		// given
		UserPrincipal principal = TokenFixtures.principal(1L);
		tokenService.issue(principal);

		// when
		tokenService.invalidateAll(principal.userId(), null);

		// then
		assertThat(refreshTokenStore.getSavedHashOrNull(principal.userId(), TokenFixtures.DEFAULT_SESSION_ID)).isNull();
	}

	@Test
	@DisplayName("액세스가 있으면 무효화 시 블랙리스트에 등록됨")
	void invalidateAll_whenAccessProvided_thenJtiIsBlacklisted() {
		// given
		UserPrincipal principal = TokenFixtures.principal(1L);
		TokenIssuer.IssuedTokens tokens = tokenService.issue(principal);
		String jti = tokenIssuer.extractJtiFromAccessToken(tokens.accessToken());

		// when
		tokenService.invalidateAll(principal.userId(), tokens.accessToken());

		// then
		assertThat(blacklistStore.isBlacklisted(jti)).isTrue();
	}
}
