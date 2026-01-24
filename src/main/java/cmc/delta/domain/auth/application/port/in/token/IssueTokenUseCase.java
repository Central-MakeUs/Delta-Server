package cmc.delta.domain.auth.application.port.in.token;

import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.global.config.security.principal.UserPrincipal;

public interface IssueTokenUseCase {
	TokenIssuer.IssuedTokens issue(UserPrincipal principal);
}
