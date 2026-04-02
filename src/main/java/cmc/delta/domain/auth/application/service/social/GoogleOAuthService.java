package cmc.delta.domain.auth.application.service.social;

import cmc.delta.domain.auth.adapter.out.oauth.google.GoogleIdTokenVerifier;
import cmc.delta.domain.auth.adapter.out.oauth.google.GoogleOAuthClient;
import cmc.delta.domain.auth.adapter.out.oauth.google.GoogleTokenResponse;
import cmc.delta.domain.auth.application.exception.SocialAuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

	private final GoogleOAuthClient googleOAuthClient;
	private final GoogleIdTokenVerifier googleIdTokenVerifier;

	public SocialUserInfo fetchUserInfoByCode(String code) {
		GoogleTokenResponse tokenResponse = googleOAuthClient.exchangeCode(code);
		GoogleIdTokenVerifier.GoogleIdClaims claims = googleIdTokenVerifier.verifyAndExtract(tokenResponse.idToken());

		String providerUserId = requireProvided(claims.sub(), "소셜 사용자 식별자가 비어있습니다.");
		String email = requireProvided(claims.email(), "소셜 이메일 제공 동의가 필요합니다.");
		String nickname = requireProvided(claims.name(), "소셜 프로필(닉네임) 제공 동의가 필요합니다.");

		return new SocialUserInfo(providerUserId, email, nickname);
	}

	private String requireProvided(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw SocialAuthException.invalidRequest(message);
		}
		return value;
	}

	public record SocialUserInfo(String providerUserId, String email, String nickname) {
	}
}
