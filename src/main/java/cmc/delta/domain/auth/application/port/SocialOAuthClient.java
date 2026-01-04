package cmc.delta.domain.auth.application.port;

public interface SocialOAuthClient {

	OAuthToken exchangeCode(String code);

	OAuthProfile fetchProfile(String providerAccessToken);

	record OAuthToken(String accessToken) {
	}

	record OAuthProfile(String providerUserId, String email, String nickname) {
	}
}
