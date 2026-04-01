package cmc.delta.domain.auth.adapter.out.oauth.google;

import cmc.delta.domain.auth.adapter.out.oauth.client.OAuthClientException;
import cmc.delta.domain.auth.adapter.out.oauth.client.OAuthHttpClient;
import cmc.delta.domain.auth.application.port.out.SocialOAuthClient;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/** 구글 OAuth 서버와 통신해 토큰 교환/프로필 조회를 수행한다. */
public class GoogleOAuthClient implements SocialOAuthClient {

	private static final String PROVIDER_NAME = "google";
	private static final String OP_TOKEN = OAuthClientException.OP_TOKEN_EXCHANGE;
	private static final String OP_USER = OAuthClientException.OP_PROFILE_FETCH;

	private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
	private static final String USER_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
	private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

	private final GoogleOAuthProperties properties;
	private final OAuthHttpClient oauthHttpClient;

	public GoogleOAuthClient(GoogleOAuthProperties properties, OAuthHttpClient oauthHttpClient) {
		this.properties = properties;
		this.oauthHttpClient = oauthHttpClient;
	}

	@Override
	public OAuthToken exchangeCode(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE);
		form.add("client_id", properties.clientId());
		form.add("client_secret", properties.clientSecret());
		form.add("redirect_uri", properties.redirectUri());
		form.add("code", code);

		GoogleTokenResponse body = oauthHttpClient.postForm(
			PROVIDER_NAME,
			OP_TOKEN,
			TOKEN_URL,
			form,
			GoogleTokenResponse.class);

		if (body == null || !StringUtils.hasText(body.accessToken())) {
			throw OAuthClientException.tokenExchangeInvalidResponse(PROVIDER_NAME);
		}

		return new OAuthToken(body.accessToken());
	}

	@Override
	public OAuthProfile fetchProfile(String providerAccessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(providerAccessToken);

		GoogleUserResponse body = oauthHttpClient.get(
			PROVIDER_NAME,
			OP_USER,
			USER_URL,
			headers,
			GoogleUserResponse.class);

		if (body == null || !StringUtils.hasText(body.id())) {
			throw OAuthClientException.profileFetchInvalidResponse(PROVIDER_NAME);
		}

		return new OAuthProfile(body.id(), body.email(), body.name());
	}
}
