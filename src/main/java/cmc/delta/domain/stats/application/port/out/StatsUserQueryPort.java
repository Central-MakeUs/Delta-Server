package cmc.delta.domain.stats.application.port.out;

import cmc.delta.domain.user.model.enums.UserStatus;
import java.time.LocalDateTime;

public interface StatsUserQueryPort {
	long countAll();
	long countByStatus(UserStatus status);
	long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
