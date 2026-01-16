package cmc.delta.domain.problem.api.scan.dto.response;

import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import java.time.LocalDateTime;

public record ProblemScanSummaryResponse(
	Long scanId,
	ScanStatus status,
	boolean hasFigure,
	RenderMode renderMode,
	OriginalImageResponse originalImage,
	ProblemScanSummaryClassificationResponse classification,
	LocalDateTime createdAt,
	LocalDateTime aiCompletedAt
) {
	public record OriginalImageResponse(
		Long assetId,
		String viewUrl,
		Integer width,
		Integer height
	) { }
}
