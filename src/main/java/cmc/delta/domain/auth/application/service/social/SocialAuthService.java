package cmc.delta.domain.auth.application.service.social;

import cmc.delta.domain.auth.adapter.out.oauth.redis.RedisLoginKeyStore;
import cmc.delta.domain.auth.application.exception.SocialAuthException;
import cmc.delta.domain.auth.application.port.in.loginkey.LoginKeyExchangeUseCase;
import cmc.delta.global.config.FrontendProperties;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

	private final LoginKeyExchangeUseCase loginKeyExchangeUseCase;
	private final FrontendProperties frontendProperties;

	public String createLoginKeyAndBuildRedirect(
		cmc.delta.domain.auth.application.port.in.social.SocialLoginCommandUseCase.LoginResult result,
		Duration ttl) {
		String loginKey = UUID.randomUUID().toString();
		loginKeyExchangeUseCase.save(loginKey, result.data(), result.tokens(), ttl);

		String base = frontendProperties.baseUrl() != null && !frontendProperties.baseUrl().isBlank()
			? frontendProperties.baseUrl()
			: "http://localhost:3000";

		return base + "/oauth/apple/callback?loginKey=" + loginKey;
	}

	public RedisLoginKeyStore.Stored consumeLoginKey(String loginKey) {
		RedisLoginKeyStore.Stored stored = loginKeyExchangeUseCase.exchange(loginKey);
		if (stored == null) {
			throw SocialAuthException.invalidRequest("invalid_or_expired_login_key");
		}
		return stored;
	}
}
