package cmc.delta.domain.auth.application;

import cmc.delta.domain.auth.api.dto.KakaoLoginData;
import cmc.delta.domain.auth.application.port.SocialOAuthClient;
import cmc.delta.domain.auth.application.port.TokenIssuer;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 카카오 로그인 플로우를 오케스트레이션. */
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

	private static final String DEFAULT_ROLE = "USER"; // role claim용(실서비스는 DB 조회로 교체)

	private final SocialOAuthClient socialOAuthClient;
	private final TokenIssuer tokenIssuer;

	public LoginResult loginWithCode(String code) {
		SocialOAuthClient.OAuthToken oauthToken = socialOAuthClient.exchangeCode(code);
		SocialOAuthClient.OAuthProfile profile = socialOAuthClient.fetchProfile(oauthToken.accessToken());

		String email = requireText(profile.email(), "카카오 이메일 제공 동의가 필요합니다.");
		String nickname = requireText(profile.nickname(), "카카오 프로필(닉네임) 제공 동의가 필요합니다.");

		// 후에: providerUserId로 내부 userId를 조회/생성하고 그 userId로 principal 구성하면 됨.
		long userId = parseUserId(profile.providerUserId());

		UserPrincipal principal = new UserPrincipal(userId, DEFAULT_ROLE);
		TokenIssuer.IssuedTokens tokens = tokenIssuer.issue(principal);

		return new LoginResult(KakaoLoginData.of(email, nickname, false), tokens);
	}

	private long parseUserId(String providerUserId) {
		try {
			return Long.parseLong(providerUserId);
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "소셜 사용자 식별자 처리 실패");
		}
	}

	private String requireText(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, message);
		}
		return value;
	}

	public record LoginResult(KakaoLoginData data, TokenIssuer.IssuedTokens tokens) {
	}
}
