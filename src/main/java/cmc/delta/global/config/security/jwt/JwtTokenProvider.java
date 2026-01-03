package cmc.delta.global.config.security.jwt;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import cmc.delta.global.config.security.principal.UserPrincipal;
import org.springframework.stereotype.Component;

import cmc.delta.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/** Access JWT를 발급/검증/파싱한다. */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYP = "typ";
    private static final String TYP_ACCESS = "access";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = createSigningKey(properties.secretBase64());
    }

    /** Access 토큰을 발급한다. */
    public String issueAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.accessTtlSeconds());
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(principal.userId()))
                .id(jti)
                .claim(CLAIM_ROLE, principal.role())
                .claim(CLAIM_TYP, TYP_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /** Access 토큰을 파싱하고 필요한 정보를 반환한다(실패 시 예외). */
    public ParsedAccessToken parseAccessTokenOrThrow(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtAuthenticationException(ErrorCode.TOKEN_REQUIRED);
        }

        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();
            validateAccessType(claims);

            Long userId = parseUserId(claims.getSubject());
            String role = claims.get(CLAIM_ROLE, String.class);
            String jti = claims.getId();
            Instant expiresAt = toInstant(claims.getExpiration());

            validateRequiredFields(role, jti, expiresAt);

            return new ParsedAccessToken(new UserPrincipal(userId, role), jti, expiresAt);

        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtAuthenticationException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }
    }

    private SecretKey createSigningKey(String secretBase64) {
        // base64 secret을 HS256 키로 변환한다.
        byte[] keyBytes = Decoders.BASE64.decode(secretBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private void validateAccessType(Claims claims) {
        // access 토큰만 통과시킨다.
        String typ = claims.get(CLAIM_TYP, String.class);
        if (!TYP_ACCESS.equals(typ)) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }
    }

    private Long parseUserId(String subject) {
        // subject를 userId(Long)로 파싱한다.
        try {
            return Long.parseLong(subject);
        } catch (Exception e) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }
    }

    private Instant toInstant(Date date) {
        // 만료 시간을 Instant로 변환한다.
        if (date == null) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }
        return date.toInstant();
    }

    private void validateRequiredFields(String role, String jti, Instant expiresAt) {
        // 필수 클레임 누락을 방지한다.
        if (role == null || role.isBlank()) throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        if (jti == null || jti.isBlank()) throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        if (expiresAt == null) throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
    }

    /** 필터에서 사용할 파싱 결과를 담는다. */
    public record ParsedAccessToken(UserPrincipal principal, String jti, Instant expiresAt) {}
}
