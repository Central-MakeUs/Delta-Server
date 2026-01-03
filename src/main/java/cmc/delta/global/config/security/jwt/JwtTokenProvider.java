package cmc.delta.global.config.security.jwt;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYP = "typ";

    private static final String TYP_ACCESS = "access";
    private static final String TYP_REFRESH = "refresh";

    // refresh TTL 설정을 properties에 넣기 전(최소 변경)에는 의미 있는 상수로 둔다.
    private static final long DEFAULT_REFRESH_TTL_SECONDS = 60L * 60 * 24 * 14; // 14 days

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = createSigningKey(properties.secretBase64());
    }

    /** Access 토큰을 발급한다. */
    public String issueAccessToken(UserPrincipal principal) {
        return issueToken(principal, TYP_ACCESS, properties.accessTtlSeconds());
    }

    /** Refresh 토큰을 발급한다. (최소 변경: TTL은 상수, 필요 시 properties로 승격) */
    public String issueRefreshToken(UserPrincipal principal) {
        return issueToken(principal, TYP_REFRESH, DEFAULT_REFRESH_TTL_SECONDS);
    }

    /** Access 토큰을 파싱하고 필요한 정보를 반환한다(실패 시 예외). */
    public ParsedAccessToken parseAccessTokenOrThrow(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtAuthenticationException(ErrorCode.TOKEN_REQUIRED);
        }

        try {
            Claims claims = parseClaims(token);
            validateTokenType(claims, TYP_ACCESS, ErrorCode.INVALID_TOKEN);

            Long userId = parseUserId(claims.getSubject(), ErrorCode.INVALID_TOKEN);
            String role = claims.get(CLAIM_ROLE, String.class);
            String jti = claims.getId();
            Instant expiresAt = toInstant(claims.getExpiration(), ErrorCode.INVALID_TOKEN);

            validateRequiredFields(role, jti, expiresAt, ErrorCode.INVALID_TOKEN);

            return new ParsedAccessToken(new UserPrincipal(userId, role), jti, expiresAt);

        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtAuthenticationException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }
    }

    /** Refresh 토큰을 파싱하고 필요한 정보를 반환한다(실패 시 예외). */
    public ParsedRefreshToken parseRefreshTokenOrThrow(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtAuthenticationException(ErrorCode.REFRESH_TOKEN_REQUIRED);
        }

        try {
            Claims claims = parseClaims(token);
            validateTokenType(claims, TYP_REFRESH, ErrorCode.INVALID_REFRESH_TOKEN);

            Long userId = parseUserId(claims.getSubject(), ErrorCode.INVALID_REFRESH_TOKEN);
            String role = claims.get(CLAIM_ROLE, String.class);
            String jti = claims.getId();
            Instant expiresAt = toInstant(claims.getExpiration(), ErrorCode.INVALID_REFRESH_TOKEN);

            validateRequiredFields(role, jti, expiresAt, ErrorCode.INVALID_REFRESH_TOKEN);

            return new ParsedRefreshToken(new UserPrincipal(userId, role), jti, expiresAt);

        } catch (ExpiredJwtException e) {
            // refresh 만료 전용 코드가 없으니 정책상 INVALID_REFRESH_TOKEN로 처리
            throw new JwtAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN);
        } catch (JwtAuthenticationException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
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

    private Claims parseClaims(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
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

    private void validateRequiredFields(String role, String jti, Instant expiresAt, ErrorCode errorCode) {
        if (role == null || role.isBlank()) throw new JwtAuthenticationException(errorCode);
        if (jti == null || jti.isBlank()) throw new JwtAuthenticationException(errorCode);
        if (expiresAt == null) throw new JwtAuthenticationException(errorCode);
    }

    /** 필터에서 사용할 Access 파싱 결과를 담는다. */
    public record ParsedAccessToken(UserPrincipal principal, String jti, Instant expiresAt) {}

    /** 필터/재발급에서 사용할 Refresh 파싱 결과를 담는다. */
    public record ParsedRefreshToken(UserPrincipal principal, String jti, Instant expiresAt) {}
}
