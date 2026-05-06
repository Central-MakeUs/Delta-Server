package cmc.delta.domain.stats.application.service;

import cmc.delta.domain.stats.application.port.in.RecordUserAccessUseCase;
import cmc.delta.domain.stats.application.port.out.UserDailyAccessRecordPort;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccessRecordService implements RecordUserAccessUseCase {

	private final UserDailyAccessRecordPort userDailyAccessRecordPort;

	@Override
	@Transactional
	public void record(Long userId) {
		userDailyAccessRecordPort.recordIfAbsent(userId, LocalDate.now());
	}
}
