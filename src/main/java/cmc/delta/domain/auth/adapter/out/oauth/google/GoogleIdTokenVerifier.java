package cmc.delta.domain.auth.adapter.out.oauth.google;

import cmc.delta.global.error.exception.BusinessException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import java.net.URL;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GoogleIdTokenVerifier {

	private static final String ISSUER_1 = "https://accounts.google.com";
	private static final String ISSUER_2 = "accounts.google.com";
	private static final String JWK_URL = "https://www.googleapis.com/oauth2/v3/certs";
	private static final long JWK_CACHE_TTL_SECONDS = 600L;

	private final GoogleOAuthProperties props;
	private final Clock clock;
	private final JwkSetLoader jwkSetLoader;

	private volatile JWKSet cachedJwkSet;
	private volatile long cachedAtEpochSec;

	@Autowired
	public GoogleIdTokenVerifier(GoogleOAuthProperties props) {
		this(props, Clock.systemUTC(), url -> JWKSet.load(new URL(url)));
	}

	GoogleIdTokenVerifier(GoogleOAuthProperties props, Clock clock, JwkSetLoader jwkSetLoader) {
		this.props = props;
		this.clock = clock;
		this.jwkSetLoader = jwkSetLoader;
	}

	public GoogleIdClaims verifyAndExtract(String idToken) {
		if (!StringUtils.hasText(idToken)) {
			throw GoogleIdTokenException.idTokenEmpty();
		}

		SignedJWT jwt = parse(idToken);

		validateClaims(jwt);
		verifySignature(jwt);

		String sub = getStringClaim(jwt, "sub");
		String email = getStringClaim(jwt, "email");
		String name = getStringClaim(jwt, "name");

		if (!StringUtils.hasText(sub)) {
			throw GoogleIdTokenException.subEmpty();
		}

		return new GoogleIdClaims(sub, email, name);
	}

	private SignedJWT parse(String idToken) {
		try {
			return SignedJWT.parse(idToken);
		} catch (ParseException e) {
			throw GoogleIdTokenException.idTokenParseFailed(e);
		}
	}

	private void validateClaims(SignedJWT jwt) {
		try {
			String iss = jwt.getJWTClaimsSet().getIssuer();
			if (!ISSUER_1.equals(iss) && !ISSUER_2.equals(iss)) {
				throw GoogleIdTokenException.issuerInvalid();
			}

			if (jwt.getJWTClaimsSet().getAudience() == null
				|| !jwt.getJWTClaimsSet().getAudience().contains(props.clientId())) {
				throw GoogleIdTokenException.audienceInvalid();
			}

			Date exp = jwt.getJWTClaimsSet().getExpirationTime();
			if (exp == null || exp.toInstant().isBefore(Instant.now(clock))) {
				throw GoogleIdTokenException.tokenExpired();
			}

		} catch (ParseException e) {
			throw GoogleIdTokenException.claimReadFailed(e);
		}
	}

	private void verifySignature(SignedJWT jwt) {
		try {
			String kid = jwt.getHeader().getKeyID();
			if (!StringUtils.hasText(kid)) {
				throw GoogleIdTokenException.kidEmpty();
			}

			JWKSet jwkSet = loadJwkSet();
			JWK jwk = jwkSet.getKeyByKeyId(kid);
			if (jwk == null) {
				invalidateCache();
				jwkSet = loadJwkSet();
				jwk = jwkSet.getKeyByKeyId(kid);
			}
			if (jwk == null) {
				throw GoogleIdTokenException.publicKeyNotFound();
			}

			if (!(jwk instanceof RSAKey)) {
				throw GoogleIdTokenException.publicKeyTypeNotRsa(String.valueOf(jwk.getKeyType()));
			}

			if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
				throw GoogleIdTokenException.algorithmNotRs256();
			}

			RSAKey rsaKey = (RSAKey)jwk;
			boolean ok = jwt.verify(new RSASSAVerifier(rsaKey.toRSAPublicKey()));
			if (!ok) {
				throw GoogleIdTokenException.signatureVerifyFailed();
			}

		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			throw GoogleIdTokenException.verifyUnexpectedError(e);
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
		long now = Instant.now(clock).getEpochSecond();
		if (cachedJwkSet != null && (now - cachedAtEpochSec) < JWK_CACHE_TTL_SECONDS) {
			return cachedJwkSet;
		}
		try {
			JWKSet jwkSet = jwkSetLoader.load(JWK_URL);
			cachedJwkSet = jwkSet;
			cachedAtEpochSec = now;
			return jwkSet;
		} catch (Exception e) {
			throw GoogleIdTokenException.jwkLoadFailed(e);
		}
	}

	@FunctionalInterface
	interface JwkSetLoader {
		JWKSet load(String url) throws Exception;
	}

	private void invalidateCache() {
		cachedJwkSet = null;
		cachedAtEpochSec = 0L;
	}

	public static record GoogleIdClaims(String sub, String email, String name) {
	}
}
