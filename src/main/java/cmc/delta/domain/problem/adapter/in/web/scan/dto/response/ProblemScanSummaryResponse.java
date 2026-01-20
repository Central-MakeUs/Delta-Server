package cmc.delta.domain.problem.adapter.in.web.scan.dto.response;

import cmc.delta.domain.problem.model.enums.ScanStatus;

public record ProblemScanSummaryResponse(
	Long scanId,
	ScanStatus status,
	OriginalImage originalImage,
	ProblemScanSummaryClassificationResponse classification
) {
	public record OriginalImage(
		Long assetId,
		String viewUrl
	) { }
}
