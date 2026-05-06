package cmc.delta.domain.stats.application.port.out;

import java.time.LocalDate;

public interface UserDailyAccessRecordPort {

	void recordIfAbsent(Long userId, LocalDate date);
}
