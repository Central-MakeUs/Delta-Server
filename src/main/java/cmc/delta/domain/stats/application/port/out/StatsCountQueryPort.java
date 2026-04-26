package cmc.delta.domain.stats.application.port.out;

import java.time.LocalDateTime;

public interface StatsCountQueryPort {
	PeriodStatsCountResult countAll(LocalDateTime from, LocalDateTime to);
}
