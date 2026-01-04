package cmc.delta.domain.auth.infrastructure.oauth.client;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import cmc.delta.global.error.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

/**
 * 외부 OAuth 통신 예외를 우리 예외(ErrorCode 정책)로 매핑한다.
 *
 * - provider 4xx(잘못된 code/토큰 등) -> UnauthorizedException(AUTHENTICATION_FAILED, WARN)
 * - provider 5xx/timeout -> INTERNAL_ERROR(ERROR)로 처리(필요하면 추후 전용 코드 추가)
 */
@Slf4j
@Component
public class OAuthClientExceptionMapper {

	public RuntimeException mapHttpStatus(String providerName, String operation, HttpStatusCodeException e) {
		int status = e.getStatusCode().value();

		if (e.getStatusCode().is4xxClientError()) {
			return new UnauthorizedException();
		}

		return new BusinessException(
			ErrorCode.INTERNAL_ERROR,
			providerName + " " + operation + " 실패 (status=" + status + ")"
		);
	}

	public RuntimeException mapTimeout(String providerName, String operation, ResourceAccessException e) {
		return new BusinessException(
			ErrorCode.INTERNAL_ERROR,
			providerName + " " + operation + " 타임아웃"
		);
	}
}
