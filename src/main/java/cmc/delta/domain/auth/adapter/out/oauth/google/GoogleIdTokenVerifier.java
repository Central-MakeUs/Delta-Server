package cmc.delta.domain.auth.adapter.out.oauth.google;

import cmc.delta.domain.auth.adapter.out.oauth.oidc.AbstractOidcIdTokenVerifier;
import cmc.delta.domain.auth.adapter.out.oauth.oidc.OidcJwkSetLoader;
import cmc.delta.domain.auth.adapter.out.oauth.oidc.OidcVerifyFailureFactory;
import cmc.delta.global.error.exception.BusinessException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URL;
import java.time.Clock;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GoogleIdTokenVerifier extends AbstractOidcIdTokenVerifier {

	private static final Set<String> ALLOWED_ISSUERS = Set.of(
		"https://accounts.google.com", "accounts.google.com");
	private static final String JWK_URL = "https://www.googleapis.com/oauth2/v3/certs";

	private static final OidcVerifyFailureFactory FAILURES = new OidcVerifyFailureFactory() {
		@Override
		public BusinessException idTokenEmpty() {
			return GoogleIdTokenException.idTokenEmpty();
		}

		@Override
		public BusinessException idTokenParseFailed(Throwable cause) {
			return GoogleIdTokenException.idTokenParseFailed(cause);
		}

		@Override
		public BusinessException claimReadFailed(Throwable cause) {
			return GoogleIdTokenException.claimReadFailed(cause);
		}

		@Override
		public BusinessException issuerInvalid() {
			return GoogleIdTokenException.issuerInvalid();
		}

		@Override
		public BusinessException audienceInvalid() {
			return GoogleIdTokenException.audienceInvalid();
		}

		@Override
		public BusinessException tokenExpired() {
			return GoogleIdTokenException.tokenExpired();
		}

		@Override
		public BusinessException kidEmpty() {
			return GoogleIdTokenException.kidEmpty();
		}

		@Override
		public BusinessException publicKeyNotFound() {
			return GoogleIdTokenException.publicKeyNotFound();
		}

		@Override
		public BusinessException publicKeyTypeNotRsa(String keyType) {
			return GoogleIdTokenException.publicKeyTypeNotRsa(keyType);
		}

		@Override
		public BusinessException algorithmNotRs256() {
			return GoogleIdTokenException.algorithmNotRs256();
		}

		@Override
		public BusinessException signatureVerifyFailed() {
			return GoogleIdTokenException.signatureVerifyFailed();
		}

		@Override
		public BusinessException verifyUnexpectedError(Throwable cause) {
			return GoogleIdTokenException.verifyUnexpectedError(cause);
		}

		@Override
		public BusinessException jwkLoadFailed(Throwable cause) {
			return GoogleIdTokenException.jwkLoadFailed(cause);
		}
	};

	private final GoogleOAuthProperties props;

	@Autowired
	public GoogleIdTokenVerifier(GoogleOAuthProperties props) {
		this(props, Clock.systemUTC(), url -> JWKSet.load(new URL(url)));
	}

	GoogleIdTokenVerifier(GoogleOAuthProperties props, Clock clock, OidcJwkSetLoader jwkSetLoader) {
		super(clock, jwkSetLoader, ALLOWED_ISSUERS, JWK_URL);
		this.props = props;
	}

	public GoogleIdClaims verifyAndExtract(String idToken) {
		SignedJWT jwt = parseAndVerify(idToken);

		String sub = findStringClaim(jwt, CLAIM_SUB).orElse(null);
		if (!StringUtils.hasText(sub)) {
			throw GoogleIdTokenException.subEmpty();
		}

		return new GoogleIdClaims(
			sub,
			findStringClaim(jwt, CLAIM_EMAIL).orElse(null),
			findStringClaim(jwt, CLAIM_NAME).orElse(null));
	}

	@Override
	protected OidcVerifyFailureFactory failures() {
		return FAILURES;
	}

	@Override
	protected String expectedAudience() {
		return props.clientId();
	}

	public static record GoogleIdClaims(String sub, String email, String name) {
	}
}
