package cmc.delta.domain.problem.adapter.in.web.scan.dto.response;

public record ProblemScanSummaryClassificationResponse(
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	CurriculumItemResponse type,
	boolean needsReview
) { }
