package cmc.delta.domain.auth.adapter.out.oauth.apple;

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
import org.springframework.web.client.RestTemplate;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

@Component
public class AppleOAuthClient {

	private static final String TOKEN_URL = "https://appleid.apple.com/auth/token";
	private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

	private final AppleOAuthProperties props;
	private final RestTemplate appleRestTemplate;

	public AppleOAuthClient(AppleOAuthProperties props, RestTemplate appleRestTemplate) {
		this.props = props;
		this.appleRestTemplate = appleRestTemplate;
	}

	public AppleTokenResponse exchangeCode(String code) {
		if (!StringUtils.hasText(code)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "애플 authorization code가 비어있습니다.");
		}

		String clientSecretJwt = generateClientSecret();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.add("client_id", props.clientId());
		form.add("client_secret", clientSecretJwt);
		form.add("code", code);
		form.add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE);
		form.add("redirect_uri", props.redirectUri());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<MultiValueMap<String, String>> entity =
			new HttpEntity<MultiValueMap<String, String>>(form, headers);

		ResponseEntity<AppleTokenResponse> resp =
			appleRestTemplate.exchange(TOKEN_URL, HttpMethod.POST, entity, AppleTokenResponse.class);

		AppleTokenResponse body = resp.getBody();
		if (body == null || !StringUtils.hasText(body.idToken())) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "애플 토큰 교환 응답이 비어있습니다.");
		}
		return body;
	}

	/**
	 * client_secret = ES256로 서명한 JWT
	 * iss = team_id
	 * sub = client_id (Services ID)
	 * aud = https://appleid.apple.com
	 */

	private String generateClientSecret() {
		try {
			Instant now = Instant.now();
			Instant exp = now.plusSeconds(300);

			JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.issuer(props.teamId())
				.subject(props.clientId())
				.audience("https://appleid.apple.com")
				.issueTime(java.util.Date.from(now))
				.expirationTime(java.util.Date.from(exp))
				.build();

			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
				.keyID(props.keyId())
				.type(JOSEObjectType.JWT)
				.build();

			SignedJWT jwt = new SignedJWT(header, claims);

			ECPrivateKey privateKey = (ECPrivateKey) loadPrivateKeyFromPem(props.privateKey());
			JWSSigner signer = new ECDSASigner(privateKey);

			jwt.sign(signer);
			return jwt.serialize();

		} catch (Exception e) {
			throw new BusinessException(
				ErrorCode.INTERNAL_ERROR,
				"애플 client_secret 생성 실패: " + e.getClass().getSimpleName()
			);
		}
	}

	private PrivateKey loadPrivateKeyFromPem(String pem) throws Exception {
		if (!StringUtils.hasText(pem)) {
			throw new IllegalStateException("apple private key is empty");
		}

		// env에서 "\n" 문자열로 넣었을 때 실제 개행으로 복구
		pem = pem.replace("\\n", "\n");

		// BEGIN/END가 본문에 붙어서 들어오는 케이스 보정
		pem = pem.replace("-----BEGIN PRIVATE KEY-----", "-----BEGIN PRIVATE KEY-----\n");
		pem = pem.replace("-----END PRIVATE KEY-----", "\n-----END PRIVATE KEY-----");

		String normalized = pem
			.replace("-----BEGIN PRIVATE KEY-----", "")
			.replace("-----END PRIVATE KEY-----", "")
			.replaceAll("\\s", "");

		byte[] der;
		try {
			der = Base64.getDecoder().decode(normalized);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid base64 in apple private key", e);
		}

		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
		KeyFactory kf = KeyFactory.getInstance("EC");
		return kf.generatePrivate(spec);
	}
	/**
	 * 애플 토큰 응답(JSON)
	 * id_token은 OpenID Connect ID Token(JWT)
	 */
	public static record AppleTokenResponse(
		String access_token,
		String token_type,
		Long expires_in,
		String refresh_token,
		String id_token
	) {
		public String idToken() { return id_token; }
		public String accessToken() { return access_token; }
		public String refreshToken() { return refresh_token; }
	}
}
