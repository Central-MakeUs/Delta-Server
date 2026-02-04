package cmc.delta.domain.auth.application.service.loginkey;

import cmc.delta.domain.auth.adapter.out.oauth.loginkey.RedisLoginKeyStore;
import cmc.delta.domain.auth.application.port.in.loginkey.LoginKeyExchangeUseCase;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginKeyExchangeServiceImpl implements LoginKeyExchangeUseCase {

	private final RedisLoginKeyStore loginKeyStore;

	@Override
	public void save(String loginKey, cmc.delta.domain.auth.application.port.in.social.SocialLoginData data,
		cmc.delta.domain.auth.application.port.out.TokenIssuer.IssuedTokens tokens, Duration ttl) {
		// RedisLoginKeyStore에 loginKey 페이로드를 저장합니다.
		loginKeyStore.save(loginKey, data, tokens, ttl);
	}

	@Override
	public RedisLoginKeyStore.Stored exchange(String loginKey) {
		// Redis에서 원자적으로 읽고 삭제합니다.
		return loginKeyStore.consume(loginKey);
	}
}
