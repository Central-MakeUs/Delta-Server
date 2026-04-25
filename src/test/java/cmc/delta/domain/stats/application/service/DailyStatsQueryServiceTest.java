package cmc.delta.domain.stats.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import cmc.delta.domain.problem.adapter.out.persistence.problem.ProblemAiSolutionTaskJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.ScanRepository;
import cmc.delta.domain.stats.application.dto.DailyStatsReport;
import cmc.delta.domain.user.adapter.out.persistence.jpa.UserJpaRepository;
import cmc.delta.domain.user.model.enums.UserStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyStatsQueryServiceTest {

	@Mock
	private UserJpaRepository userJpaRepository;

	@Mock
	private ScanRepository scanRepository;

	@Mock
	private ProblemJpaRepository problemJpaRepository;

	@Mock
	private ProblemAiSolutionTaskJpaRepository aiSolutionTaskJpaRepository;

	@Mock
	private Clock clock;

	@InjectMocks
	private DailyStatsQueryService sut;

	private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 4, 24, 21, 0, 0);

	@BeforeEach
	void setUp() {
		given(clock.instant()).willReturn(FIXED_NOW.atZone(ZoneId.systemDefault()).toInstant());
		given(clock.getZone()).willReturn(ZoneId.systemDefault());
		given(userJpaRepository.count()).willReturn(0L);
		given(userJpaRepository.countByStatus(UserStatus.WITHDRAWN)).willReturn(0L);
		given(userJpaRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);
		given(scanRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);
		given(problemJpaRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);
		given(aiSolutionTaskJpaRepository.countByRequestedAtBetween(any(), any())).willReturn(0L);
	}

	@Test
	@DisplayName("오늘/3일/7일 기간별 통계를 각각 집계한다")
	void generate_returnsPeriodStatsForAllWindows() {
		given(userJpaRepository.countByCreatedAtBetween(any(), any())).willReturn(5L, 15L, 30L);
		given(scanRepository.countByCreatedAtBetween(any(), any())).willReturn(20L, 60L, 120L);
		given(problemJpaRepository.countByCreatedAtBetween(any(), any())).willReturn(10L, 30L, 70L);
		given(aiSolutionTaskJpaRepository.countByRequestedAtBetween(any(), any())).willReturn(3L, 9L, 20L);

		DailyStatsReport report = sut.generate();

		assertThat(report.generatedAt()).isEqualTo(FIXED_NOW);
		assertThat(report.today().newUsers()).isEqualTo(5L);
		assertThat(report.today().scans()).isEqualTo(20L);
		assertThat(report.today().wrongAnswerCards()).isEqualTo(10L);
		assertThat(report.today().aiSolutionAttempts()).isEqualTo(3L);
		assertThat(report.last3Days().newUsers()).isEqualTo(15L);
		assertThat(report.last7Days().newUsers()).isEqualTo(30L);
	}

	@Test
	@DisplayName("오늘 기간의 from은 어제 00:00, to는 지금이다")
	void generate_todayPeriod_fromAndToAreToday() {
		DailyStatsReport report = sut.generate();

		assertThat(report.today().from()).isEqualTo(FIXED_TODAY.minusDays(1).atStartOfDay());
		assertThat(report.today().to()).isEqualTo(FIXED_NOW);
	}

	@Test
	@DisplayName("최근 3일 기간의 from은 3일 전 00:00, to는 지금이다")
	void generate_last3DaysPeriod_hasCorrectDateRange() {
		DailyStatsReport report = sut.generate();

		assertThat(report.last3Days().from()).isEqualTo(FIXED_TODAY.minusDays(3).atStartOfDay());
		assertThat(report.last3Days().to()).isEqualTo(FIXED_NOW);
	}

	@Test
	@DisplayName("최근 7일 기간의 from은 7일 전 00:00, to는 지금이다")
	void generate_last7DaysPeriod_hasCorrectDateRange() {
		DailyStatsReport report = sut.generate();

		assertThat(report.last7Days().from()).isEqualTo(FIXED_TODAY.minusDays(7).atStartOfDay());
		assertThat(report.last7Days().to()).isEqualTo(FIXED_NOW);
	}

	@Test
	@DisplayName("모든 지표가 0이어도 정상 리포트를 생성한다")
	void generate_allZero_returnsReportWithZeros() {
		DailyStatsReport report = sut.generate();

		assertThat(report.today().newUsers()).isZero();
		assertThat(report.today().scans()).isZero();
		assertThat(report.today().wrongAnswerCards()).isZero();
		assertThat(report.today().aiSolutionAttempts()).isZero();
	}
}
