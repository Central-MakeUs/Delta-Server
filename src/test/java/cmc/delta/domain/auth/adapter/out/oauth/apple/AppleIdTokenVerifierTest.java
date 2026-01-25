package cmc.delta.domain.auth.adapter.out.oauth.apple;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppleIdTokenVerifierTest {

	private static final String CLIENT_ID = "client-id";
	private static final String ISSUER = "https://appleid.apple.com";

	@Test
	@DisplayName("정상 토큰이면 sub/email을 추출")
	void verifyAndExtract_ok() throws Exception {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);
		KeyPair kp = generateRsa();
		RSAKey jwk = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) kp.getPublic())
			.keyID("kid1")
			.build();
		JWKSet jwkSet = new JWKSet(jwk);

		AppleIdTokenVerifier verifier = new AppleIdTokenVerifier(
			props(),
			clock,
			url -> jwkSet);

		String token = signJwt(
			kp,
			new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("kid1").build(),
			new JWTClaimsSet.Builder()
				.issuer(ISSUER)
				.audience(CLIENT_ID)
				.expirationTime(Date.from(now.plusSeconds(60)))
				.claim("sub", "sub")
				.claim("email", "a@b.com")
				.build());

		AppleIdTokenVerifier.AppleIdClaims claims = verifier.verifyAndExtract(token);
		assertThat(claims.sub()).isEqualTo("sub");
		assertThat(claims.email()).isEqualTo("a@b.com");
	}

	@Test
	@DisplayName("idToken이 비어있으면 INVALID_REQUEST")
	void verifyAndExtract_whenEmpty_thenInvalidRequest() {
		AppleIdTokenVerifier verifier = new AppleIdTokenVerifier(props());
		BusinessException ex = catchThrowableOfType(
			() -> verifier.verifyAndExtract(" "),
			BusinessException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("파싱 실패면 AUTHENTICATION_FAILED")
	void verifyAndExtract_whenParseFail_thenAuthFailed() {
		AppleIdTokenVerifier verifier = new AppleIdTokenVerifier(props());
		BusinessException ex = catchThrowableOfType(
			() -> verifier.verifyAndExtract("not-a-jwt"),
			BusinessException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
	}

	@Test
	@DisplayName("issuer가 다르면 AUTHENTICATION_FAILED")
	void verifyAndExtract_whenIssuerInvalid_thenAuthFailed() throws Exception {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);
		KeyPair kp = generateRsa();
		RSAKey jwk = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) kp.getPublic())
			.keyID("kid1")
			.build();
		JWKSet jwkSet = new JWKSet(jwk);
		AppleIdTokenVerifier verifier = new AppleIdTokenVerifier(props(), clock, url -> jwkSet);

		String token = signJwt(
			kp,
			new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("kid1").build(),
			new JWTClaimsSet.Builder()
				.issuer("https://invalid")
				.audience(CLIENT_ID)
				.expirationTime(Date.from(now.plusSeconds(60)))
				.claim("sub", "sub")
				.build());

		BusinessException ex = catchThrowableOfType(
			() -> verifier.verifyAndExtract(token),
			BusinessException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
	}

	@Test
	@DisplayName("kid가 없으면 AUTHENTICATION_FAILED")
	void verifyAndExtract_whenKidMissing_thenAuthFailed() throws Exception {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);
		KeyPair kp = generateRsa();
		RSAKey jwk = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) kp.getPublic())
			.keyID("kid1")
			.build();
		JWKSet jwkSet = new JWKSet(jwk);
		AppleIdTokenVerifier verifier = new AppleIdTokenVerifier(props(), clock, url -> jwkSet);

		String token = signJwt(
			kp,
			new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
			new JWTClaimsSet.Builder()
				.issuer(ISSUER)
				.audience(CLIENT_ID)
				.expirationTime(Date.from(now.plusSeconds(60)))
				.claim("sub", "sub")
				.build());

		BusinessException ex = catchThrowableOfType(
			() -> verifier.verifyAndExtract(token),
			BusinessException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
	}

	@Test
	@DisplayName("서명 검증 실패면 AUTHENTICATION_FAILED")
	void verifyAndExtract_whenSignatureInvalid_thenAuthFailed() throws Exception {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);
		KeyPair signer = generateRsa();
		KeyPair jwkKey = generateRsa();
		RSAKey jwk = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) jwkKey.getPublic())
			.keyID("kid1")
			.build();
		JWKSet jwkSet = new JWKSet(jwk);
		AppleIdTokenVerifier verifier = new AppleIdTokenVerifier(props(), clock, url -> jwkSet);

		String token = signJwt(
			signer,
			new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("kid1").build(),
			new JWTClaimsSet.Builder()
				.issuer(ISSUER)
				.audience(CLIENT_ID)
				.expirationTime(Date.from(now.plusSeconds(60)))
				.claim("sub", "sub")
				.build());

		BusinessException ex = catchThrowableOfType(
			() -> verifier.verifyAndExtract(token),
			BusinessException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
	}

	private AppleOAuthProperties props() {
		return new AppleOAuthProperties(
			CLIENT_ID,
			"team-id",
			"key-id",
			"private-key",
			"redirect",
			1000,
			1000);
	}

	private KeyPair generateRsa() throws Exception {
		KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
		gen.initialize(2048);
		return gen.generateKeyPair();
	}

	private String signJwt(KeyPair kp, JWSHeader header, JWTClaimsSet claims) throws Exception {
		SignedJWT jwt = new SignedJWT(header, claims);
		jwt.sign(new RSASSASigner((java.security.interfaces.RSAPrivateKey) kp.getPrivate()));
		return jwt.serialize();
	}
}
