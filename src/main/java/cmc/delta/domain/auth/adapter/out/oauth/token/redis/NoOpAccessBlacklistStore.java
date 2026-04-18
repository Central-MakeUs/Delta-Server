package cmc.delta.domain.auth.adapter.out.oauth.token.redis;

import cmc.delta.domain.auth.application.port.out.AccessBlacklistStore;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "jwt.blacklist", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAccessBlacklistStore implements AccessBlacklistStore {

	@Override
	public boolean isBlacklisted(String jti) {
		return false;
	}

	@Override
	public void blacklist(String jti, Duration ttl) {}
}
