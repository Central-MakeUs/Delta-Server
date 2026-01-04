package cmc.delta.domain.auth.infrastructure.redis;

import cmc.delta.domain.auth.application.port.AccessBlacklistStore;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 블랙리스트를 사용하지 않을 때의 기본 구현체다. */
@Component
@ConditionalOnProperty(prefix = "jwt.blacklist", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAccessBlacklistStore implements AccessBlacklistStore {
	// 블랙리스트 미사용 시 항상 false로 처리한다.
	@Override
	public boolean isBlacklisted(String jti) {
		return false;
	}

	// 블랙리스트 미사용 시 아무 것도 하지 않는다.
	@Override
	public void blacklist(String jti, Duration ttl) {}
}
