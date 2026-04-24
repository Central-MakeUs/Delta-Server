package cmc.delta.domain.stats.application.dto;

import java.time.LocalDate;

public record PeriodStats(
	LocalDate from,
	LocalDate to,
	long newUsers,
	long scans,
	long wrongAnswerCards,
	long aiSolutionAttempts
) {}
