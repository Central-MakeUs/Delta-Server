package cmc.delta.domain.problem.application.port.in.scan.result;

import cmc.delta.domain.problem.model.enums.ScanStatus;

public record ProblemScanSummaryResponse(
	Long scanId,
	ScanStatus status,
	OriginalImage originalImage,
	ProblemScanSummaryClassificationResponse classification,
	String failReason) {
	public record OriginalImage(
		Long assetId,
		String viewUrl) {
	}
}
