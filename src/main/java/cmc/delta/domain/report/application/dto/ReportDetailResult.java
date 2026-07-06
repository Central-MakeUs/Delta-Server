package cmc.delta.domain.report.application.dto;

import java.time.LocalDateTime;

public record ReportDetailResult(
	Long reportId,
	String status,
	long problemCount,
	LocalDateTime generatedAt,
	ReportAggregate aggregate,
	ReportNarrative narrative) {
}
