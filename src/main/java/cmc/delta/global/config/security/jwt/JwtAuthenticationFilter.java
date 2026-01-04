package cmc.delta.global.config.security.jwt;

import cmc.delta.domain.auth.application.port.AccessBlacklistStore;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final BearerTokenResolver tokenResolver;
	private final JwtTokenProvider tokenProvider;
	private final AccessBlacklistStore blacklistStore;
	private final JwtProperties jwtProperties;

	public JwtAuthenticationFilter(
		BearerTokenResolver tokenResolver,
		JwtTokenProvider tokenProvider,
		AccessBlacklistStore blacklistStore,
		JwtProperties jwtProperties) {
		this.tokenResolver = tokenResolver;
		this.tokenProvider = tokenProvider;
		this.blacklistStore = blacklistStore;
		this.jwtProperties = jwtProperties;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		String token = tokenResolver.resolveBearerToken(request);

		if (token == null) {
			// 토큰이 없으면 Security가 permitAll/entrypoint로 처리한다.
			filterChain.doFilter(request, response);
			return;
		}

		try {
			JwtTokenProvider.ParsedAccessToken parsed = tokenProvider.parseAccessTokenOrThrow(token);

			if (isBlacklistEnabled() && blacklistStore.isBlacklisted(parsed.jti())) {
				throw new JwtAuthenticationException(ErrorCode.BLACKLISTED_TOKEN);
			}

			setAuthentication(parsed.principal());

			filterChain.doFilter(request, response);

		} catch (JwtAuthenticationException ex) {
			// 4xx는 WARN스택 남김, entrypoint로 위임.
			clearAuthentication();
			logWarnAuthFail(request, ex);
			throw ex;
		} catch (Exception ex) {
			clearAuthentication();
			log.error(
				"event=auth_error traceId={} path={} message={}",
				MDC.get("traceId"),
				request.getRequestURI(),
				ex.getMessage(),
				ex);
			throw ex;
		}
	}

	private boolean isBlacklistEnabled() {
		return jwtProperties.blacklist() != null && jwtProperties.blacklist().enabled();
	}

	// principal/authority로 Authentication을 구성해 SecurityContext에 넣는다.
	private void setAuthentication(UserPrincipal principal) {
		var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()));
		var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private void clearAuthentication() {
		SecurityContextHolder.clearContext();
	}

	// 토큰 값은 절대 로깅하지 않고 원인 코드만 남긴다.
	private void logWarnAuthFail(HttpServletRequest request, JwtAuthenticationException ex) {
		log.warn(
			"event=auth_fail traceId={} path={} reasonCode={}",
			MDC.get("traceId"),
			request.getRequestURI(),
			ex.getErrorCode().code());
	}
}
