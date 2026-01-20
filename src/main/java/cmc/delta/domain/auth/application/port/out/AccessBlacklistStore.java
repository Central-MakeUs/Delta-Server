package cmc.delta.domain.auth.application.port.out;

import java.time.Duration;

/** access 토큰 jti 블랙리스트를 관리한다. */
public interface AccessBlacklistStore {
	boolean isBlacklisted(String jti);

	void blacklist(String jti, Duration ttl);
}
