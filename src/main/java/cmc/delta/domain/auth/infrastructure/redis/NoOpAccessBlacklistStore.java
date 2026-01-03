package cmc.delta.domain.auth.infrastructure.redis;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cmc.delta.domain.auth.application.port.AccessBlacklistStore;

/** 블랙리스트를 사용하지 않을 때의 기본 구현체다. */
@Component
@ConditionalOnProperty(prefix = "jwt.blacklist", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAccessBlacklistStore implements AccessBlacklistStore {

    @Override
    public boolean isBlacklisted(String jti) {
        // 블랙리스트 미사용 시 항상 false로 처리한다.
        return false;
    }

    @Override
    public void blacklist(String jti, Duration ttl) {
        // 블랙리스트 미사용 시 아무 것도 하지 않는다.
    }
}
