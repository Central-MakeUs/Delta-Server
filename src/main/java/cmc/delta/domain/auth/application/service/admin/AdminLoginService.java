package cmc.delta.domain.auth.application.service.admin;

import cmc.delta.domain.auth.application.exception.TokenException;
import cmc.delta.domain.auth.application.port.in.admin.AdminLoginUseCase;
import cmc.delta.domain.auth.application.port.in.token.TokenCommandUseCase;
import cmc.delta.domain.auth.application.port.out.AdminUserQueryPort;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminLoginService implements AdminLoginUseCase {

	private final AdminUserQueryPort adminUserQueryPort;
	private final TokenCommandUseCase tokenCommandUseCase;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional(readOnly = true)
	public TokenIssuer.IssuedTokens login(String username, String password) {
		User admin = adminUserQueryPort.findAdminByUsername(username)
			.orElseThrow(() -> new TokenException(ErrorCode.AUTHENTICATION_FAILED));

		if (!passwordEncoder.matches(password, admin.getPassword())) {
			throw new TokenException(ErrorCode.AUTHENTICATION_FAILED);
		}

		return tokenCommandUseCase.issue(new UserPrincipal(admin.getId(), admin.getRole().name()));
	}
}
