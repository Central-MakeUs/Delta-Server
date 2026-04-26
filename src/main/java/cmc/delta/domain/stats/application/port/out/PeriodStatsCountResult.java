package cmc.delta.domain.stats.application.port.out;

public record PeriodStatsCountResult(
	long newUsers,
	long scans,
	long problems,
	long aiSolutionAttempts
) {}
