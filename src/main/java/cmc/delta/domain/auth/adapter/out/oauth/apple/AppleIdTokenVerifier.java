package cmc.delta.domain.auth.adapter.out.oauth.apple;

import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

@Component
public class AppleIdTokenVerifier {

	private static final String ISSUER = "https://appleid.apple.com";
	private static final String JWK_URL = "https://appleid.apple.com/auth/keys";

	private final AppleOAuthProperties props;

	private volatile JWKSet cachedJwkSet;
	private volatile long cachedAtEpochSec;

	public AppleIdTokenVerifier(AppleOAuthProperties props) {
		this.props = props;
	}

	public AppleIdClaims verifyAndExtract(String idToken) {
		if (!StringUtils.hasText(idToken)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "애플 id_token이 비어있습니다.");
		}

		SignedJWT jwt = parse(idToken);

		validateClaims(jwt);
		verifySignature(jwt);

		String sub = getStringClaim(jwt, "sub");
		String email = getStringClaim(jwt, "email");

		if (!StringUtils.hasText(sub)) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 sub가 비어있습니다.");
		}

		return new AppleIdClaims(sub, email);
	}

	private SignedJWT parse(String idToken) {
		try {
			return SignedJWT.parse(idToken);
		} catch (ParseException e) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 id_token 파싱에 실패했습니다.");
		}
	}

	private void validateClaims(SignedJWT jwt) {
		try {
			String iss = jwt.getJWTClaimsSet().getIssuer();
			if (!ISSUER.equals(iss)) {
				throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 iss가 올바르지 않습니다.");
			}

			if (jwt.getJWTClaimsSet().getAudience() == null
				|| !jwt.getJWTClaimsSet().getAudience().contains(props.clientId())) {
				throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 aud가 올바르지 않습니다.");
			}

			Date exp = jwt.getJWTClaimsSet().getExpirationTime();
			if (exp == null || exp.toInstant().isBefore(Instant.now())) {
				throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 id_token이 만료되었습니다.");
			}

		} catch (ParseException e) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 클레임 조회에 실패했습니다.");
		}
	}

	private void verifySignature(SignedJWT jwt) {
		try {
			String kid = jwt.getHeader().getKeyID();
			if (!StringUtils.hasText(kid)) {
				throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 토큰 헤더 kid가 비어있습니다.");
			}

			JWKSet jwkSet = loadJwkSet();
			JWK jwk = jwkSet.getKeyByKeyId(kid);
			if (jwk == null) {
				invalidateCache();
				jwkSet = loadJwkSet();
				jwk = jwkSet.getKeyByKeyId(kid);
			}
			if (jwk == null) {
				throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 공개키(kid)에 해당하는 키를 찾지 못했습니다.");
			}

			if (!(jwk instanceof RSAKey)) {
				throw new BusinessException(
					ErrorCode.AUTHENTICATION_FAILED,
					"애플 공개키 타입이 RSA가 아닙니다: " + jwk.getKeyType()
				);
			}

			if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
				throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 토큰 알고리즘이 RS256이 아닙니다.");
			}

			RSAKey rsaKey = (RSAKey) jwk;
			boolean ok = jwt.verify(new RSASSAVerifier(rsaKey.toRSAPublicKey()));
			if (!ok) {
				throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 id_token 서명 검증에 실패했습니다.");
			}

		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 id_token 검증 중 오류가 발생했습니다.");
		}
	}

	private String getStringClaim(SignedJWT jwt, String name) {
		try {
			Object v = jwt.getJWTClaimsSet().getClaim(name);
			return (v == null) ? null : String.valueOf(v);
		} catch (ParseException e) {
			return null;
		}
	}

	private JWKSet loadJwkSet() {
		long now = Instant.now().getEpochSecond();
		if (cachedJwkSet != null && (now - cachedAtEpochSec) < 600) {
			return cachedJwkSet;
		}
		try {
			JWKSet jwkSet = JWKSet.load(new URL(JWK_URL));
			cachedJwkSet = jwkSet;
			cachedAtEpochSec = now;
			return jwkSet;
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "애플 공개키(JWK) 로딩에 실패했습니다.");
		}
	}

	private void invalidateCache() {
		cachedJwkSet = null;
		cachedAtEpochSec = 0L;
	}

	public static record AppleIdClaims(String sub, String email) {}
}
