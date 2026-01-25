package cmc.delta.domain.problem.application.port.in.scan.result;

import java.util.List;

import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;

public record ProblemScanSummaryClassificationResponse(
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	List<CurriculumItemResponse> types,
	boolean needsReview
) { }
