package cmc.delta.domain.auth.application.support;

import cmc.delta.global.config.security.principal.UserPrincipal;

public final class AuthPrincipalFactory {

	private AuthPrincipalFactory() {}

	public static UserPrincipal principalOf(long userId) {
		return new UserPrincipal(userId, AuthRoleDefaults.DEFAULT_ROLE_FOR_DEV);
	}
}
