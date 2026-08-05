package cmc.delta.domain.auth.adapter.out.oauth.apple;

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
public class AppleIdTokenVerifier extends AbstractOidcIdTokenVerifier {

	private static final Set<String> ALLOWED_ISSUERS = Set.of("https://appleid.apple.com");
	private static final String JWK_URL = "https://appleid.apple.com/auth/keys";

	private static final OidcVerifyFailureFactory FAILURES = new OidcVerifyFailureFactory() {
		@Override
		public BusinessException idTokenEmpty() {
			return AppleOAuthException.idTokenEmpty();
		}

		@Override
		public BusinessException idTokenParseFailed(Throwable cause) {
			return AppleOAuthException.idTokenParseFailed(cause);
		}

		@Override
		public BusinessException claimReadFailed(Throwable cause) {
			return AppleOAuthException.claimReadFailed(cause);
		}

		@Override
		public BusinessException issuerInvalid() {
			return AppleOAuthException.issuerInvalid();
		}

		@Override
		public BusinessException audienceInvalid() {
			return AppleOAuthException.audienceInvalid();
		}

		@Override
		public BusinessException tokenExpired() {
			return AppleOAuthException.tokenExpired();
		}

		@Override
		public BusinessException kidEmpty() {
			return AppleOAuthException.kidEmpty();
		}

		@Override
		public BusinessException publicKeyNotFound() {
			return AppleOAuthException.publicKeyNotFound();
		}

		@Override
		public BusinessException publicKeyTypeNotRsa(String keyType) {
			return AppleOAuthException.publicKeyTypeNotRsa(keyType);
		}

		@Override
		public BusinessException algorithmNotRs256() {
			return AppleOAuthException.algorithmNotRs256();
		}

		@Override
		public BusinessException signatureVerifyFailed() {
			return AppleOAuthException.signatureVerifyFailed();
		}

		@Override
		public BusinessException verifyUnexpectedError(Throwable cause) {
			return AppleOAuthException.verifyUnexpectedError(cause);
		}

		@Override
		public BusinessException jwkLoadFailed(Throwable cause) {
			return AppleOAuthException.jwkLoadFailed(cause);
		}
	};

	private final AppleOAuthProperties props;

	@Autowired
	public AppleIdTokenVerifier(AppleOAuthProperties props) {
		this(props, Clock.systemUTC(), url -> JWKSet.load(new URL(url)));
	}

	AppleIdTokenVerifier(AppleOAuthProperties props, Clock clock, OidcJwkSetLoader jwkSetLoader) {
		super(clock, jwkSetLoader, ALLOWED_ISSUERS, JWK_URL);
		this.props = props;
	}

	public AppleIdClaims verifyAndExtract(String idToken) {
		SignedJWT jwt = parseAndVerify(idToken);

		String sub = findStringClaim(jwt, CLAIM_SUB).orElse(null);
		if (!StringUtils.hasText(sub)) {
			throw AppleOAuthException.subEmpty();
		}

		return new AppleIdClaims(sub, findStringClaim(jwt, CLAIM_EMAIL).orElse(null));
	}

	@Override
	protected OidcVerifyFailureFactory failures() {
		return FAILURES;
	}

	@Override
	protected String expectedAudience() {
		return props.clientId();
	}

	public static record AppleIdClaims(String sub, String email) {
	}
}
