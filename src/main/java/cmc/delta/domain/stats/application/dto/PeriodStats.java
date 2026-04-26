package cmc.delta.domain.stats.application.dto;

import java.time.LocalDateTime;

public record PeriodStats(
	LocalDateTime from,
	LocalDateTime to,
	long newUsers,
	long scans,
	long wrongAnswerCards,
	long aiSolutionAttempts
) {}
