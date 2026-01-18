package cmc.delta.domain.problem.api.scan.dto.response;

public record ProblemScanSummaryClassificationResponse(
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	CurriculumItemResponse type,
	boolean needsReview
) { }
