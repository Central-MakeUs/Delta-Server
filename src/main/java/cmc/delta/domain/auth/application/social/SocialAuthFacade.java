package cmc.delta.domain.auth.application.social;

import cmc.delta.domain.auth.api.dto.response.SocialLoginData;
import cmc.delta.domain.auth.application.port.SocialOAuthClient;
import cmc.delta.domain.auth.application.port.TokenIssuer;
import cmc.delta.domain.auth.application.token.TokenService;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 소셜 로그인 플로우를 오케스트레이션. */
@Service
@RequiredArgsConstructor
public class SocialAuthFacade {
	// TODO: 실서비스는 DB 조회/Role 정책으로 교체
	private static final String DEFAULT_ROLE_FOR_DEV = "USER";

	private final SocialOAuthClient socialOAuthClient;
	private final TokenService tokenService;

	public LoginResult loginWithCode(String code) {
		SocialOAuthClient.OAuthToken oauthToken = socialOAuthClient.exchangeCode(code);
		SocialOAuthClient.OAuthProfile profile = socialOAuthClient.fetchProfile(oauthToken.accessToken());

		String email = requireProvided(profile.email(), "소셜 이메일 제공 동의가 필요합니다.");
		String nickname = requireProvided(profile.nickname(), "소셜 프로필(닉네임) 제공 동의가 필요합니다.");

		// TODO: 실서비스는 providerUserId로 내부 userId를 조회/생성하고, 그 userId로 principal 구성
		long userId = issueTemporaryUserId(profile.providerUserId());

		UserPrincipal principal = new UserPrincipal(userId, DEFAULT_ROLE_FOR_DEV);

		TokenIssuer.IssuedTokens tokens = tokenService.issue(principal);

		return new LoginResult(new SocialLoginData(email, nickname, false), tokens);
	}

	private long issueTemporaryUserId(String providerUserId) {
		if (!StringUtils.hasText(providerUserId)) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "소셜 사용자 식별자가 비어있습니다.");
		}

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(providerUserId.getBytes(StandardCharsets.UTF_8));

			long value = ByteBuffer.wrap(hash, 0, Long.BYTES).getLong() & Long.MAX_VALUE;
			return (value == 0L) ? 1L : value;

		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "소셜 사용자 식별자 처리 실패");
		}
	}

	private String requireProvided(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, message);
		}
		return value;
	}

	public record LoginResult(SocialLoginData data, TokenIssuer.IssuedTokens tokens) {
	}
}
