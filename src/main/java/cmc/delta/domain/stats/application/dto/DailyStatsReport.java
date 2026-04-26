package cmc.delta.domain.stats.application.dto;

import java.time.LocalDateTime;

public record DailyStatsReport(
	LocalDateTime generatedAt,
	long totalUsers,
	long withdrawnUsers,
	PeriodStats today,
	PeriodStats last3Days,
	PeriodStats last7Days
) {}
