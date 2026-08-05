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

		ErrorCode blockReason = resolveBlockReason(request);
		if (blockReason != null) {
			throw new JwtAuthenticationException(blockReason);
		}

		filterChain.doFilter(request, response);
	}

	// 차단 사유가 없으면 null을 반환해 필터 체인을 계속 진행시킨다.
	private ErrorCode resolveBlockReason(HttpServletRequest request) {
		UserPrincipal principal = findAuthenticatedPrincipal();
		if (principal == null) {
			return null;
		}

		UserStatus status = userStatusQuery.getStatus(principal.userId());
		if (status == UserStatus.WITHDRAWN) {
			return matches(WITHDRAWN_ALLOWLIST, request) ? null : ErrorCode.USER_WITHDRAWN;
		}

		return resolveOnboardingBlock(status, request);
	}

	private UserPrincipal findAuthenticatedPrincipal() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
			return null;
		}
		return principal;
	}

	private ErrorCode resolveOnboardingBlock(UserStatus status, HttpServletRequest request) {
		if (matches(ALLOWLIST, request)) {
			return null;
		}
		return status == UserStatus.ONBOARDING_REQUIRED ? ErrorCode.USER_ONBOARDING_REQUIRED : null;
	}

	private boolean matches(Set<String> allowlist, HttpServletRequest request) {
		String key = request.getMethod() + " " + request.getRequestURI();
		return allowlist.contains(key);
	}
}
