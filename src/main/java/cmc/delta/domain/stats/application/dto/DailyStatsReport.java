package cmc.delta.domain.stats.application.dto;

import java.time.LocalDateTime;

public record DailyStatsReport(
	LocalDateTime generatedAt,
	PeriodStats today,
	PeriodStats last3Days,
	PeriodStats last7Days
) {}
