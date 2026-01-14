package cmc.delta.domain.problem.api.dto.response;

import java.time.LocalDateTime;

public record ProblemScanDetailResponse(
	Long scanId,
	String status,
	boolean hasFigure,
	String renderMode,

	OriginalImage originalImage,

	String ocrPlainText,
	String aiProblemLatex,
	String aiSolutionLatex,

	LocalDateTime createdAt,
	LocalDateTime ocrCompletedAt,
	LocalDateTime aiCompletedAt,

	String failReason
) {
	public record OriginalImage(
		Long assetId,
		String viewUrl,
		Integer width,
		Integer height
	) {}
}
