package cmc.delta.domain.problem.application.port.in.scan.result;

import java.time.LocalDateTime;
import java.util.List;

public record ProblemScanDetailResponse(
	Long scanId,
	String status,
	boolean hasFigure,
	String renderMode,

	OriginalImage originalImage,

	String ocrPlainText,
	String aiProblemLatex,
	String aiSolutionLatex,

	AiClassification ai,

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
	) {
	}

	public record AiClassification(
		String subjectId,
		String subjectName,
		String unitId,
		String unitName,
		String typeId,
		String typeName,
		Double confidence,
		Boolean needsReview,
		List<PredictedTypeResponse> predictedTypes,
		String unitCandidatesJson,
		String typeCandidatesJson,
		String aiDraftJson
	) {
	}

	public record PredictedTypeResponse(
		String typeId,
		String typeName,
		Integer rankNo,
		java.math.BigDecimal confidence
	) {}
}
