package cmc.delta.global.config.security.jwt;

import cmc.delta.domain.user.application.port.in.UserStatusQuery;
import cmc.delta.domain.user.model.enums.UserStatus;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class OnboardingBlockFilter extends OncePerRequestFilter {

	private final UserStatusQuery userStatusQuery;

	private static final Set<String> ALLOWLIST = Set.of(
		"POST /api/v1/users/me/onboarding",
		"POST /api/v1/users/withdrawal",
		"GET /api/v1/users/me",
		"POST /api/v1/auth/logout",
		"POST /api/v1/auth/reissue");

	private static final Set<String> WITHDRAWN_ALLOWLIST = Set.of(
		"POST /api/v1/users/withdrawal",
		"POST /api/v1/auth/logout");

	public OnboardingBlockFilter(UserStatusQuery userStatusQuery) {
		this.userStatusQuery = userStatusQuery;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
			filterChain.doFilter(request, response);
			return;
		}

		UserStatus status = userStatusQuery.getStatus(principal.userId());
		if (status == UserStatus.WITHDRAWN) {
			if (isWithdrawnAllowed(request)) {
				filterChain.doFilter(request, response);
				return;
			}
			throw new JwtAuthenticationException(ErrorCode.USER_WITHDRAWN);
		}

		if (isAllowed(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		if (status == UserStatus.ONBOARDING_REQUIRED) {
			throw new JwtAuthenticationException(ErrorCode.USER_ONBOARDING_REQUIRED);
		}

		filterChain.doFilter(request, response);
	}

	private boolean isAllowed(HttpServletRequest request) {
		String key = request.getMethod() + " " + request.getRequestURI();
		return ALLOWLIST.contains(key);
	}

	private boolean isWithdrawnAllowed(HttpServletRequest request) {
		String key = request.getMethod() + " " + request.getRequestURI();
		return WITHDRAWN_ALLOWLIST.contains(key);
	}
}
