package cmc.delta.domain.auth.adapter.out.oauth.apple;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class AppleOAuthClient {

	private static final String TOKEN_URL = "https://appleid.apple.com/auth/token";
	private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
	private static final long CLIENT_SECRET_TTL_SECONDS = 300L;

	private final AppleOAuthProperties props;
	private final RestTemplate appleRestTemplate;

	public AppleOAuthClient(AppleOAuthProperties props, RestTemplate appleRestTemplate) {
		this.props = props;
		this.appleRestTemplate = appleRestTemplate;
	}

	public AppleTokenResponse exchangeCode(String code) {
		if (!StringUtils.hasText(code)) {
			throw AppleOAuthException.authorizationCodeEmpty();
		}
		String clientSecretJwt = generateClientSecret();
		HttpEntity<MultiValueMap<String, String>> entity = buildTokenRequestBody(code, clientSecretJwt);
		try {
			ResponseEntity<AppleTokenResponse> resp = appleRestTemplate.exchange(TOKEN_URL, HttpMethod.POST, entity,
				AppleTokenResponse.class);
			return requireValidTokenResponse(resp.getBody());
		} catch (HttpStatusCodeException e) {
			int status = e.getStatusCode().value();
			throw AppleOAuthException.tokenExchangeFailed(status, e);
		} catch (ResourceAccessException e) {
			throw AppleOAuthException.tokenExchangeTimeout(e);
		}
	}

	private HttpEntity<MultiValueMap<String, String>> buildTokenRequestBody(String code, String clientSecretJwt) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.add("client_id", props.clientId());
		form.add("client_secret", clientSecretJwt);
		form.add("code", code);
		form.add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE);
		form.add("redirect_uri", props.redirectUri());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		return new HttpEntity<MultiValueMap<String, String>>(form, headers);
	}

	private AppleTokenResponse requireValidTokenResponse(AppleTokenResponse body) {
		if (body == null || !StringUtils.hasText(body.idToken())) {
			throw AppleOAuthException.tokenExchangeInvalidResponse();
		}
		return body;
	}

	// client_secret = ES256로 서명한 JWT
	// iss = team_id
	// sub = client_id (Services ID)
	// aud = https://appleid.apple.com
	private String generateClientSecret() {
		try {
			JWTClaimsSet claims = buildClientSecretClaims();
			JWSHeader header = buildClientSecretHeader();
			return signClientSecret(new SignedJWT(header, claims));
		} catch (Exception e) {
			throw AppleOAuthException.clientSecretGenerateFailed(e);
		}
	}

	private JWTClaimsSet buildClientSecretClaims() {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(CLIENT_SECRET_TTL_SECONDS);

		return new JWTClaimsSet.Builder()
			.issuer(props.teamId())
			.subject(props.clientId())
			.audience("https://appleid.apple.com")
			.issueTime(java.util.Date.from(now))
			.expirationTime(java.util.Date.from(exp))
			.build();
	}

	private JWSHeader buildClientSecretHeader() {
		return new JWSHeader.Builder(JWSAlgorithm.ES256)
			.keyID(props.keyId())
			.type(JOSEObjectType.JWT)
			.build();
	}

	private String signClientSecret(SignedJWT jwt) throws Exception {
		ECPrivateKey privateKey = (ECPrivateKey)loadPrivateKeyFromPem(props.privateKey());
		JWSSigner signer = new ECDSASigner(privateKey);

		jwt.sign(signer);
		return jwt.serialize();
	}

	private PrivateKey loadPrivateKeyFromPem(String pem) throws Exception {
		if (!StringUtils.hasText(pem)) {
			throw AppleOAuthException.privateKeyEmpty();
		}

		String normalized = normalizePemBody(pem);

		byte[] der;
		try {
			der = Base64.getDecoder().decode(normalized);
		} catch (IllegalArgumentException e) {
			throw AppleOAuthException.privateKeyInvalidBase64(e);
		}

		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
		KeyFactory kf = KeyFactory.getInstance("EC");
		return kf.generatePrivate(spec);
	}

	private String normalizePemBody(String pem) {
		String unescaped = pem.replace("\\n", "\n");

		// BEGIN/END가 본문에 붙어서 들어오는 케이스 보정
		unescaped = unescaped.replace("-----BEGIN PRIVATE KEY-----", "-----BEGIN PRIVATE KEY-----\n");
		unescaped = unescaped.replace("-----END PRIVATE KEY-----", "\n-----END PRIVATE KEY-----");

		return unescaped
			.replace("-----BEGIN PRIVATE KEY-----", "")
			.replace("-----END PRIVATE KEY-----", "")
			.replaceAll("\\s", "");
	}

	// 애플 토큰 응답(JSON)
	// id_token은 OpenID Connect ID Token(JWT)
	public static record AppleTokenResponse(
		String access_token,
		String token_type,
		Long expires_in,
		String refresh_token,
		String id_token) {
		public String idToken() {
			return id_token;
		}

		public String accessToken() {
			return access_token;
		}

		public String refreshToken() {
			return refresh_token;
		}
	}
}
