package cmc.delta.domain.auth.infrastructure.oauth.kakao;

import cmc.delta.domain.auth.application.port.SocialOAuthClient;
import cmc.delta.domain.auth.infrastructure.oauth.client.OAuthHttpClient;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/** 카카오 OAuth 서버와 통신해 토큰 교환/프로필 조회를 수행한다. */
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements SocialOAuthClient {

	private static final String PROVIDER_NAME = "kakao";
	private static final String OP_TOKEN = "토큰 교환";
	private static final String OP_USER = "유저 조회";

	private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
	private static final String USER_URL = "https://kapi.kakao.com/v2/user/me";
	private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

	private final KakaoOAuthProperties properties;

	// 카카오 전용 OAuthHttpClient Bean 사용
	private final @Qualifier("kakaoOAuthHttpClient") OAuthHttpClient oauthHttpClient;

	@Override
	public OAuthToken exchangeCode(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE);
		form.add("client_id", properties.clientId());
		form.add("redirect_uri", properties.redirectUri());
		form.add("code", code);

		if (StringUtils.hasText(properties.clientSecret())) {
			form.add("client_secret", properties.clientSecret());
		}

		KakaoTokenResponse body = oauthHttpClient.postForm(
			PROVIDER_NAME,
			OP_TOKEN,
			TOKEN_URL,
			form,
			KakaoTokenResponse.class
		);

		if (body == null || !StringUtils.hasText(body.accessToken())) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "카카오 토큰 응답이 비어있습니다.");
		}

		return new OAuthToken(body.accessToken());
	}

	@Override
	public OAuthProfile fetchProfile(String providerAccessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(providerAccessToken);

		KakaoUserResponse body = oauthHttpClient.get(
			PROVIDER_NAME,
			OP_USER,
			USER_URL,
			headers,
			KakaoUserResponse.class
		);

		if (body == null || body.id() <= 0) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "카카오 유저 응답이 비어있습니다.");
		}

		String email = (body.kakaoAccount() == null) ? null : body.kakaoAccount().email();
		String nickname = (body.kakaoAccount() != null && body.kakaoAccount().profile() != null)
			? body.kakaoAccount().profile().nickname()
			: null;

		return new OAuthProfile(String.valueOf(body.id()), email, nickname);
	}
}
