package cmc.delta.domain.auth.application.port.in.admin;

import cmc.delta.domain.auth.application.port.out.TokenIssuer;

public interface AdminLoginUseCase {

	TokenIssuer.IssuedTokens login(String username, String password);
}
