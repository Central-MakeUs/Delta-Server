package cmc.delta.domain.stats.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cmc.delta.domain.stats.application.dto.DailyStatsReport;
import cmc.delta.domain.stats.application.dto.PeriodStats;
import cmc.delta.domain.stats.application.port.out.PeriodStatsCountResult;
import cmc.delta.domain.stats.application.port.out.StatsCountQueryPort;
import cmc.delta.domain.stats.application.port.out.StatsUserQueryPort;
import cmc.delta.domain.user.model.enums.UserStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyStatsQueryService {

	private static final int PERIOD_TODAY = 1;
	private static final int PERIOD_LAST_3_DAYS = 3;
	private static final int PERIOD_LAST_7_DAYS = 7;
	private static final int INCLUSIVE_DAY_OFFSET = 1;

	private final StatsUserQueryPort userQueryPort;
	private final StatsCountQueryPort countQueryPort;
	private final Clock clock;

	@Transactional(readOnly = true)
	public DailyStatsReport generate() {
		LocalDateTime now = LocalDateTime.now(clock);

		return new DailyStatsReport(
			now,
			userQueryPort.countAllExcludingAdmin(),
			userQueryPort.countByStatusExcludingAdmin(UserStatus.WITHDRAWN),
			queryPeriod(now, PERIOD_TODAY),
			queryPeriod(now, PERIOD_LAST_3_DAYS),
			queryPeriod(now, PERIOD_LAST_7_DAYS)
		);
	}

	private PeriodStats queryPeriod(LocalDateTime now, int days) {
		LocalDateTime from = now.minusDays(days - INCLUSIVE_DAY_OFFSET).with(LocalTime.MIN);
		LocalDateTime to = now.with(LocalTime.MAX);
		PeriodStatsCountResult counts = countQueryPort.countAll(from, to);

		return new PeriodStats(from, to, counts.newUsers(), counts.scans(), counts.problems(), counts.aiSolutionAttempts());
	}
}
