package cmc.delta.domain.auth.adapter.out.oauth.client;

import cmc.delta.global.error.exception.UnauthorizedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

// 외부 OAuth 통신 예외를 우리 예외(ErrorCode 정책)로 매핑합니다.
// - provider 4xx(잘못된 code/토큰 등) -> UnauthorizedException(AUTHENTICATION_FAILED)
// - provider 5xx/timeout -> OAuthClientException(OAUTH_*)
@Component
public class OAuthClientExceptionMapper {

	public RuntimeException mapHttpStatus(String providerName, String operation, HttpStatusCodeException e) {
		int status = e.getStatusCode().value();

		if (e.getStatusCode().is4xxClientError()) {
			return new UnauthorizedException();
		}
		if (OAuthClientException.OP_TOKEN_EXCHANGE.equals(operation)) {
			return OAuthClientException.tokenExchangeError(providerName, status, e);
		}
		if (OAuthClientException.OP_PROFILE_FETCH.equals(operation)) {
			return OAuthClientException.profileFetchError(providerName, status, e);
		}
		return OAuthClientException.providerErrorFallback(providerName, operation, status, e);
	}

	public RuntimeException mapTimeout(String providerName, String operation, ResourceAccessException e) {
		if (OAuthClientException.OP_TOKEN_EXCHANGE.equals(operation)) {
			return OAuthClientException.tokenExchangeTimeout(providerName, e);
		}
		if (OAuthClientException.OP_PROFILE_FETCH.equals(operation)) {
			return OAuthClientException.profileFetchTimeout(providerName, e);
		}
		return OAuthClientException.providerTimeoutFallback(providerName, operation, e);
	}
}
