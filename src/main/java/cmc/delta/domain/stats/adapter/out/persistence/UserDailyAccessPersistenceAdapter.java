package cmc.delta.domain.stats.adapter.out.persistence;

import cmc.delta.domain.stats.application.port.out.UserDailyAccessRecordPort;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserDailyAccessPersistenceAdapter implements UserDailyAccessRecordPort {

	private final UserDailyAccessJpaRepository userDailyAccessJpaRepository;

	@Override
	public void recordIfAbsent(Long userId, LocalDate date) {
		userDailyAccessJpaRepository.insertIgnore(userId, date);
	}
}
