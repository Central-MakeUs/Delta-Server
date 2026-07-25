package cmc.delta.domain.auth.adapter.out.oauth.oidc;

import cmc.delta.global.error.exception.BusinessException;

/** 공통 OIDC 검증 로직이 던질 예외를 프로바이더별(구글/애플) 예외 타입으로 생성한다. */
public interface OidcVerifyFailureFactory {

	BusinessException idTokenEmpty();

	BusinessException idTokenParseFailed(Throwable cause);

	BusinessException claimReadFailed(Throwable cause);

	BusinessException issuerInvalid();

	BusinessException audienceInvalid();

	BusinessException tokenExpired();

	BusinessException kidEmpty();

	BusinessException publicKeyNotFound();

	BusinessException publicKeyTypeNotRsa(String keyType);

	BusinessException algorithmNotRs256();

	BusinessException signatureVerifyFailed();

	BusinessException verifyUnexpectedError(Throwable cause);

	BusinessException jwkLoadFailed(Throwable cause);
}
