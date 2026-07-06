package cmc.delta.domain.report.application.dto;

import java.time.LocalDateTime;

public record ReportRequestResult(
	Long reportId,
	String status,
	boolean reusedExisting,
	LocalDateTime requestedAt) {
}
