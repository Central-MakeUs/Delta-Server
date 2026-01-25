package cmc.delta.domain.auth.application.service.social;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cmc.delta.domain.auth.application.port.out.SocialOAuthClient;
import cmc.delta.domain.auth.application.exception.SocialAuthException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

	private final SocialOAuthClient kakaoOAuthClient; // 기존 KakaoOAuthClient Bean 주입

	public SocialUserInfo fetchUserInfoByCode(String code) {
		SocialOAuthClient.OAuthToken oauthToken = kakaoOAuthClient.exchangeCode(code);
		SocialOAuthClient.OAuthProfile profile = kakaoOAuthClient.fetchProfile(oauthToken.accessToken());

		String providerUserId = requireProvided(profile.providerUserId(), "소셜 사용자 식별자가 비어있습니다.");
		String email = requireProvided(profile.email(), "소셜 이메일 제공 동의가 필요합니다.");
		String nickname = requireProvided(profile.nickname(), "소셜 프로필(닉네임) 제공 동의가 필요합니다.");

		return new SocialUserInfo(providerUserId, email, nickname);
	}

	private String requireProvided(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw SocialAuthException.invalidRequest(message);
		}
		return value;
	}

	public record SocialUserInfo(String providerUserId, String email, String nickname) {}
}
