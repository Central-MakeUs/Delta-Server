package cmc.delta.domain.auth.infrastructure.oauth;

import cmc.delta.domain.auth.application.port.SocialOAuthClient;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import cmc.delta.global.error.exception.UnauthorizedException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/** 카카오 OAuth 서버와 통신해 토큰 교환/프로필 조회를 수행한다. */
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements SocialOAuthClient {

	private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
	private static final String USER_URL = "https://kapi.kakao.com/v2/user/me";
	private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

	private final RestTemplate kakaoRestTemplate;
	private final KakaoOAuthProperties properties;

	@Override
	public OAuthToken exchangeCode(String code) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE);
		form.add("client_id", properties.clientId());
		form.add("redirect_uri", properties.redirectUri());
		form.add("code", code);

		if (StringUtils.hasText(properties.clientSecret())) {
			form.add("client_secret", properties.clientSecret());
		}

		try {
			ResponseEntity<KakaoTokenResponse> response = kakaoRestTemplate.postForEntity(
				TOKEN_URL, new HttpEntity<>(form, headers), KakaoTokenResponse.class);

			KakaoTokenResponse body = response.getBody();
			if (body == null || !StringUtils.hasText(body.accessToken())) {
				throw new BusinessException(ErrorCode.INTERNAL_ERROR, "카카오 토큰 응답이 비어있습니다.");
			}
			return new OAuthToken(body.accessToken());

		} catch (HttpStatusCodeException e) {
			// 민감정보 로깅 금지: 여기선 throw만
			if (e.getStatusCode().is4xxClientError()) {
				throw new UnauthorizedException();
			}
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "카카오 토큰 교환 실패");

		} catch (ResourceAccessException e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "카카오 토큰 교환 타임아웃");
		}
	}

	@Override
	public OAuthProfile fetchProfile(String providerAccessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(providerAccessToken);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));

		try {
			ResponseEntity<KakaoUserResponse> response = kakaoRestTemplate.exchange(
				USER_URL, HttpMethod.GET, new HttpEntity<>(headers), KakaoUserResponse.class);

			KakaoUserResponse body = response.getBody();
			if (body == null || body.id() <= 0) {
				throw new BusinessException(ErrorCode.INTERNAL_ERROR, "카카오 유저 응답이 비어있습니다.");
			}

			String email = (body.kakaoAccount() == null) ? null : body.kakaoAccount().email();
			String nickname = (body.kakaoAccount() != null && body.kakaoAccount().profile() != null)
				? body.kakaoAccount().profile().nickname()
				: null;

			return new OAuthProfile(String.valueOf(body.id()), email, nickname);

		} catch (HttpStatusCodeException e) {
			if (e.getStatusCode().is4xxClientError()) {
				throw new UnauthorizedException();
			}
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "카카오 유저 조회 실패");

		} catch (ResourceAccessException e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "카카오 유저 조회 타임아웃");
		}
	}
}
