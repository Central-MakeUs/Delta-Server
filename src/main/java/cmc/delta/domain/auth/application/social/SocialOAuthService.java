package cmc.delta.domain.auth.application.social;

import cmc.delta.domain.auth.application.port.SocialOAuthClient;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SocialOAuthService {

	private final SocialOAuthClient socialOAuthClient;

	public SocialUserInfo fetchUserInfoByCode(String code) {
		SocialOAuthClient.OAuthToken oauthToken = socialOAuthClient.exchangeCode(code);
		SocialOAuthClient.OAuthProfile profile = socialOAuthClient.fetchProfile(oauthToken.accessToken());

		String providerUserId = requireProvided(profile.providerUserId(), "소셜 사용자 식별자가 비어있습니다.");
		String email = requireProvided(profile.email(), "소셜 이메일 제공 동의가 필요합니다.");
		String nickname = requireProvided(profile.nickname(), "소셜 프로필(닉네임) 제공 동의가 필요합니다.");

		return new SocialUserInfo(providerUserId, email, nickname);
	}

	private String requireProvided(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, message);
		}
		return value;
	}

	public record SocialUserInfo(String providerUserId, String email, String nickname) {
	}
}
