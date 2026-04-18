package cmc.delta.global.config.security.jwt;

import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private static final String CLAIM_ROLE = "role";
	private static final String CLAIM_TYP = "typ";

	private static final String TYP_ACCESS = "access";
	private static final String TYP_REFRESH = "refresh";

	private static final long DEFAULT_REFRESH_TTL_SECONDS = 60L * 60 * 24 * 14;

	private final JwtProperties properties;
	private final SecretKey signingKey;

	public JwtTokenProvider(JwtProperties properties) {
		this.properties = properties;
		this.signingKey = createSigningKey(properties.secretBase64());
	}

	public String issueAccessToken(UserPrincipal principal) {
		return issueToken(principal, TYP_ACCESS, properties.accessTtlSeconds());
	}

	public String issueRefreshToken(UserPrincipal principal) {
		return issueToken(principal, TYP_REFRESH, DEFAULT_REFRESH_TTL_SECONDS);
	}

	public ParsedAccessToken parseAccessTokenOrThrow(String token) {
		if (token == null || token.isBlank()) {
			throw new JwtAuthenticationException(ErrorCode.TOKEN_REQUIRED);
		}
		return parseAndBuild(token, TYP_ACCESS, ErrorCode.INVALID_TOKEN, ErrorCode.EXPIRED_TOKEN,
			ParsedAccessToken::new);
	}

	public ParsedRefreshToken parseRefreshTokenOrThrow(String token) {
		if (token == null || token.isBlank()) {
			throw new JwtAuthenticationException(ErrorCode.REFRESH_TOKEN_REQUIRED);
		}
		return parseAndBuild(token, TYP_REFRESH, ErrorCode.INVALID_REFRESH_TOKEN, ErrorCode.INVALID_REFRESH_TOKEN,
			ParsedRefreshToken::new);
	}

	private <T> T parseAndBuild(String token, String expectedTyp, ErrorCode invalidCode, ErrorCode expiredCode,
		TokenFactory<T> factory) {
		try {
			Claims claims = parseVerifiedClaims(token);
			validateTokenType(claims, expectedTyp, invalidCode);

			Long userId = parseUserId(claims.getSubject(), invalidCode);
			String role = claims.get(CLAIM_ROLE, String.class);
			String jti = claims.getId();
			Instant expiresAt = toInstant(claims.getExpiration(), invalidCode);

			validateRequiredFields(role, jti, expiresAt, invalidCode);
			return factory.create(new UserPrincipal(userId, role), jti, expiresAt);

		} catch (ExpiredJwtException e) {
			throw new JwtAuthenticationException(expiredCode);
		} catch (JwtAuthenticationException e) {
			throw e;
		} catch (JwtException | IllegalArgumentException e) {
			throw new JwtAuthenticationException(invalidCode);
		}
	}

	@FunctionalInterface
	private interface TokenFactory<T> {
		T create(UserPrincipal principal, String jti, Instant expiresAt);
	}

	private String issueToken(UserPrincipal principal, String typ, long ttlSeconds) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(ttlSeconds);
		String jti = UUID.randomUUID().toString();

		return Jwts.builder()
			.issuer(properties.issuer())
			.subject(String.valueOf(principal.userId()))
			.id(jti)
			.claim(CLAIM_ROLE, principal.role())
			.claim(CLAIM_TYP, typ)
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiresAt))
			.signWith(signingKey, Jwts.SIG.HS256)
			.compact();
	}

	private Claims parseVerifiedClaims(String token) {
		Jws<Claims> jws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
		return jws.getPayload();
	}

	private SecretKey createSigningKey(String secretBase64) {
		byte[] keyBytes = Decoders.BASE64.decode(secretBase64);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	private void validateTokenType(Claims claims, String expectedTyp, ErrorCode errorCode) {
		String typ = claims.get(CLAIM_TYP, String.class);
		if (!expectedTyp.equals(typ)) {
			throw new JwtAuthenticationException(errorCode);
		}
	}

	private Long parseUserId(String subject, ErrorCode errorCode) {
		try {
			return Long.parseLong(subject);
		} catch (Exception e) {
			throw new JwtAuthenticationException(errorCode);
		}
	}

	private Instant toInstant(Date date, ErrorCode errorCode) {
		if (date == null) {
			throw new JwtAuthenticationException(errorCode);
		}
		return date.toInstant();
	}

	private void validateRequiredFields(
		String role, String jti, Instant expiresAt, ErrorCode errorCode) {
		if (role == null || role.isBlank())
			throw new JwtAuthenticationException(errorCode);
		if (jti == null || jti.isBlank())
			throw new JwtAuthenticationException(errorCode);
		if (expiresAt == null)
			throw new JwtAuthenticationException(errorCode);
	}

	public record ParsedAccessToken(UserPrincipal principal, String jti, Instant expiresAt) {
	}

	public record ParsedRefreshToken(UserPrincipal principal, String jti, Instant expiresAt) {
	}
}
