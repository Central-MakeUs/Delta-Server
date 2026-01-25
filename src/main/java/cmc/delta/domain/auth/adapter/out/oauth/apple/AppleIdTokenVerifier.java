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


import cmc.delta.global.error.exception.BusinessException;

@Component
public class AppleIdTokenVerifier {

	private static final String ISSUER = "https://appleid.apple.com";
	private static final String JWK_URL = "https://appleid.apple.com/auth/keys";
	private static final long JWK_CACHE_TTL_SECONDS = 600L;

	private final AppleOAuthProperties props;

	private volatile JWKSet cachedJwkSet;
	private volatile long cachedAtEpochSec;

	public AppleIdTokenVerifier(AppleOAuthProperties props) {
		this.props = props;
	}

	public AppleIdClaims verifyAndExtract(String idToken) {
		if (!StringUtils.hasText(idToken)) {
			throw AppleOAuthException.idTokenEmpty();
		}

		SignedJWT jwt = parse(idToken);

		validateClaims(jwt);
		verifySignature(jwt);

		String sub = getStringClaim(jwt, "sub");
		String email = getStringClaim(jwt, "email");

		if (!StringUtils.hasText(sub)) {
			throw AppleOAuthException.subEmpty();
		}

		return new AppleIdClaims(sub, email);
	}

	private SignedJWT parse(String idToken) {
		try {
			return SignedJWT.parse(idToken);
		} catch (ParseException e) {
			throw AppleOAuthException.idTokenParseFailed(e);
		}
	}

	private void validateClaims(SignedJWT jwt) {
		try {
			String iss = jwt.getJWTClaimsSet().getIssuer();
			if (!ISSUER.equals(iss)) {
				throw AppleOAuthException.issuerInvalid();
			}

			if (jwt.getJWTClaimsSet().getAudience() == null
				|| !jwt.getJWTClaimsSet().getAudience().contains(props.clientId())) {
				throw AppleOAuthException.audienceInvalid();
			}

			Date exp = jwt.getJWTClaimsSet().getExpirationTime();
			if (exp == null || exp.toInstant().isBefore(Instant.now())) {
				throw AppleOAuthException.tokenExpired();
			}

		} catch (ParseException e) {
			throw AppleOAuthException.claimReadFailed(e);
		}
	}

	private void verifySignature(SignedJWT jwt) {
		try {
			String kid = jwt.getHeader().getKeyID();
			if (!StringUtils.hasText(kid)) {
				throw AppleOAuthException.kidEmpty();
			}

			JWKSet jwkSet = loadJwkSet();
			JWK jwk = jwkSet.getKeyByKeyId(kid);
			if (jwk == null) {
				invalidateCache();
				jwkSet = loadJwkSet();
				jwk = jwkSet.getKeyByKeyId(kid);
			}
			if (jwk == null) {
				throw AppleOAuthException.publicKeyNotFound();
			}

			if (!(jwk instanceof RSAKey)) {
				throw AppleOAuthException.publicKeyTypeNotRsa(String.valueOf(jwk.getKeyType()));
			}

			if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
				throw AppleOAuthException.algorithmNotRs256();
			}

			RSAKey rsaKey = (RSAKey) jwk;
			boolean ok = jwt.verify(new RSASSAVerifier(rsaKey.toRSAPublicKey()));
			if (!ok) {
				throw AppleOAuthException.signatureVerifyFailed();
			}

		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			throw AppleOAuthException.verifyUnexpectedError(e);
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
		if (cachedJwkSet != null && (now - cachedAtEpochSec) < JWK_CACHE_TTL_SECONDS) {
			return cachedJwkSet;
		}
		try {
			JWKSet jwkSet = JWKSet.load(new URL(JWK_URL));
			cachedJwkSet = jwkSet;
			cachedAtEpochSec = now;
			return jwkSet;
		} catch (Exception e) {
			throw AppleOAuthException.jwkLoadFailed(e);
		}
	}

	private void invalidateCache() {
		cachedJwkSet = null;
		cachedAtEpochSec = 0L;
	}

	public static record AppleIdClaims(String sub, String email) {}
}
