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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private static final String INTERNAL_ERROR_JSON =
		"{\"status\":500,\"code\":\"INTERNAL_ERROR\",\"data\":null,\"message\":\"internal error\"}";

	private final BearerTokenResolver tokenResolver;
	private final JwtTokenProvider tokenProvider;
	private final AccessBlacklistStore blacklistStore;
	private final JwtProperties jwtProperties;
	private final AuthenticationEntryPoint authenticationEntryPoint;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri.startsWith("/api/v1/auth/reissue")
			|| uri.startsWith("/api/v1/auth/kakao");
	}

	public JwtAuthenticationFilter(
		BearerTokenResolver tokenResolver,
		JwtTokenProvider tokenProvider,
		AccessBlacklistStore blacklistStore,
		JwtProperties jwtProperties,
		AuthenticationEntryPoint authenticationEntryPoint
	) {
		this.tokenResolver = tokenResolver;
		this.tokenProvider = tokenProvider;
		this.blacklistStore = blacklistStore;
		this.jwtProperties = jwtProperties;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {

		String token = tokenResolver.resolveBearerToken(request);

		if (token == null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			authenticateByToken(token);
			filterChain.doFilter(request, response);
		} catch (JwtAuthenticationException ex) {
			handleJwtAuthFailure(request, response, ex);
		} catch (Exception ex) {
			handleUnexpectedFailure(request, response, ex);
		}
	}

	/**
	 * 토큰을 검증하고 SecurityContext에 Authentication을 세팅한다.
	 * 실패 시 JwtAuthenticationException 또는 RuntimeException 발생.
	 */
	private void authenticateByToken(String token) {
		JwtTokenProvider.ParsedAccessToken parsed = tokenProvider.parseAccessTokenOrThrow(token);

		if (isBlacklistEnabled() && blacklistStore.isBlacklisted(parsed.jti())) {
			throw new JwtAuthenticationException(ErrorCode.BLACKLISTED_TOKEN);
		}

		setAuthentication(parsed.principal());
	}

	private void handleJwtAuthFailure(
		HttpServletRequest request,
		HttpServletResponse response,
		JwtAuthenticationException ex
	) throws IOException, ServletException {

		clearAuthentication();
		logWarnAuthFail(request, ex);

		authenticationEntryPoint.commence(request, response, ex);
	}

	private void handleUnexpectedFailure(
		HttpServletRequest request,
		HttpServletResponse response,
		Exception ex
	) throws IOException {

		clearAuthentication();
		logUnexpectedError(request, ex);

		writeInternalError(response);
	}

	private void logUnexpectedError(HttpServletRequest request, Exception ex) {
		log.error(
			"event=auth_error traceId={} path={} message={}",
			MDC.get("traceId"),
			request.getRequestURI(),
			ex.getMessage(),
			ex
		);
	}

	private void writeInternalError(HttpServletResponse response) throws IOException {
		if (response.isCommitted()) {
			return;
		}
		response.setStatus(500);
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write(INTERNAL_ERROR_JSON);
	}

	private boolean isBlacklistEnabled() {
		return jwtProperties.blacklist() != null && jwtProperties.blacklist().enabled();
	}

	private void setAuthentication(UserPrincipal principal) {
		List<SimpleGrantedAuthority> authorities =
			List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()));

		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(principal, null, authorities);

		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private void clearAuthentication() {
		SecurityContextHolder.clearContext();
	}

	private void logWarnAuthFail(HttpServletRequest request, JwtAuthenticationException ex) {
		log.warn(
			"event=auth_fail traceId={} path={} reasonCode={}",
			MDC.get("traceId"),
			request.getRequestURI(),
			ex.getErrorCode().code()
		);
	}
}
