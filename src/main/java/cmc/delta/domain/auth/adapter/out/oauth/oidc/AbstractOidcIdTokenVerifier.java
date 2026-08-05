package cmc.delta.domain.auth.adapter.out.oauth.oidc;

import cmc.delta.global.error.exception.BusinessException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * OIDC id_token 서명/클레임 검증의 단일 구현.
 * 프로바이더(구글/애플)는 발급자·JWK URL·예외 타입만 다르므로 그 차이만 하위 클래스에 위임한다.
 */
public abstract class AbstractOidcIdTokenVerifier {

	private static final long JWK_CACHE_TTL_SECONDS = 600L;

	protected static final String CLAIM_SUB = "sub";
	protected static final String CLAIM_EMAIL = "email";
	protected static final String CLAIM_NAME = "name";

	private final Clock clock;
	private final OidcJwkSetLoader jwkSetLoader;
	private final Set<String> allowedIssuers;
	private final String jwkUrl;

	private volatile JWKSet cachedJwkSet;
	private volatile long cachedAtEpochSec;

	protected AbstractOidcIdTokenVerifier(
		Clock clock, OidcJwkSetLoader jwkSetLoader, Set<String> allowedIssuers, String jwkUrl) {
		this.clock = clock;
		this.jwkSetLoader = jwkSetLoader;
		this.allowedIssuers = Set.copyOf(allowedIssuers);
		this.jwkUrl = jwkUrl;
	}

	protected abstract OidcVerifyFailureFactory failures();

	protected abstract String expectedAudience();

	/** id_token을 파싱하고 클레임·서명을 모두 검증한 뒤 JWT를 돌려준다. */
	protected final SignedJWT parseAndVerify(String idToken) {
		if (!StringUtils.hasText(idToken)) {
			throw failures().idTokenEmpty();
		}
		SignedJWT jwt = parseToken(idToken);
		validateClaims(jwt);
		verifySignature(jwt);
		return jwt;
	}

	protected final Optional<String> findStringClaim(SignedJWT jwt, String claimName) {
		try {
			Object value = jwt.getJWTClaimsSet().getClaim(claimName);
			return Optional.ofNullable(value).map(String::valueOf);
		} catch (ParseException e) {
			throw failures().claimReadFailed(e);
		}
	}

	private SignedJWT parseToken(String idToken) {
		try {
			return SignedJWT.parse(idToken);
		} catch (ParseException e) {
			throw failures().idTokenParseFailed(e);
		}
	}

	private void validateClaims(SignedJWT jwt) {
		try {
			if (!allowedIssuers.contains(jwt.getJWTClaimsSet().getIssuer())) {
				throw failures().issuerInvalid();
			}
			if (jwt.getJWTClaimsSet().getAudience() == null
				|| !jwt.getJWTClaimsSet().getAudience().contains(expectedAudience())) {
				throw failures().audienceInvalid();
			}
			Date expiration = jwt.getJWTClaimsSet().getExpirationTime();
			if (expiration == null || expiration.toInstant().isBefore(Instant.now(clock))) {
				throw failures().tokenExpired();
			}
		} catch (ParseException e) {
			throw failures().claimReadFailed(e);
		}
	}

	private void verifySignature(SignedJWT jwt) {
		try {
			RSAKey rsaKey = resolveRsaKey(jwt);
			if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
				throw failures().algorithmNotRs256();
			}
			if (!jwt.verify(new RSASSAVerifier(rsaKey.toRSAPublicKey()))) {
				throw failures().signatureVerifyFailed();
			}
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			throw failures().verifyUnexpectedError(e);
		}
	}

	private RSAKey resolveRsaKey(SignedJWT jwt) {
		String kid = jwt.getHeader().getKeyID();
		if (!StringUtils.hasText(kid)) {
			throw failures().kidEmpty();
		}

		JWK jwk = findKeyByKid(kid);
		if (jwk == null) {
			throw failures().publicKeyNotFound();
		}
		if (!(jwk instanceof RSAKey rsaKey)) {
			throw failures().publicKeyTypeNotRsa(String.valueOf(jwk.getKeyType()));
		}
		return rsaKey;
	}

	private JWK findKeyByKid(String kid) {
		JWK jwk = loadJwkSet().getKeyByKeyId(kid);
		if (jwk == null) {
			// 키 롤테이션 직후일 수 있으므로 캐시를 비우고 한 번만 재시도한다.
			invalidateCache();
			jwk = loadJwkSet().getKeyByKeyId(kid);
		}
		return jwk;
	}

	private JWKSet loadJwkSet() {
		long nowEpochSec = Instant.now(clock).getEpochSecond();
		JWKSet cached = cachedJwkSet;
		if (cached != null && (nowEpochSec - cachedAtEpochSec) < JWK_CACHE_TTL_SECONDS) {
			return cached;
		}
		try {
			JWKSet jwkSet = jwkSetLoader.load(jwkUrl);
			cachedJwkSet = jwkSet;
			cachedAtEpochSec = nowEpochSec;
			return jwkSet;
		} catch (Exception e) {
			throw failures().jwkLoadFailed(e);
		}
	}

	private void invalidateCache() {
		cachedJwkSet = null;
		cachedAtEpochSec = 0L;
	}
}
