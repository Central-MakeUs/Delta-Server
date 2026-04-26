package cmc.delta.domain.stats.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cmc.delta.domain.problem.adapter.out.persistence.problem.ProblemAiSolutionTaskJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.stats.application.dto.DailyStatsReport;
import cmc.delta.domain.stats.application.dto.PeriodStats;
import cmc.delta.domain.user.adapter.out.persistence.jpa.UserJpaRepository;
import cmc.delta.domain.user.model.enums.UserStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyStatsQueryService {

	private static final int PERIOD_ONE_DAYS = 1;
	private static final int PERIOD_LAST_3_DAYS = 3;
	private static final int PERIOD_LAST_7_DAYS = 7;

	private final UserJpaRepository userJpaRepository;
	private final ScanRepository scanRepository;
	private final ProblemJpaRepository problemJpaRepository;
	private final ProblemAiSolutionTaskJpaRepository aiSolutionTaskJpaRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public DailyStatsReport generate() {
		LocalDateTime now = LocalDateTime.now(clock);

		return new DailyStatsReport(
			now,
			userJpaRepository.count(),
			userJpaRepository.countByStatus(UserStatus.WITHDRAWN),
			queryPeriod(now, PERIOD_ONE_DAYS),
			queryPeriod(now, PERIOD_LAST_3_DAYS),
			queryPeriod(now, PERIOD_LAST_7_DAYS)
		);
	}

	private PeriodStats queryPeriod(LocalDateTime now, int days) {
		LocalDateTime periodStartAt = now.minusDays(days - PERIOD_ONE_DAYS).with(LocalTime.MIN);
		LocalDateTime periodEndAt = now.with(LocalTime.MAX);

		return new PeriodStats(
			periodStartAt,
			periodEndAt,
			userJpaRepository.countByCreatedAtBetween(periodStartAt, periodEndAt),
			scanRepository.countByCreatedAtBetween(periodStartAt, periodEndAt),
			problemJpaRepository.countByCreatedAtBetween(periodStartAt, periodEndAt),
			aiSolutionTaskJpaRepository.countByRequestedAtBetween(periodStartAt, periodEndAt)
		);
	}
}
