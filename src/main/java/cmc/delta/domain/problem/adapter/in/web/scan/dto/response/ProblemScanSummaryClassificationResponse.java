package cmc.delta.domain.problem.adapter.in.web.scan.dto.response;

import java.util.List;

public record ProblemScanSummaryClassificationResponse(
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	List<CurriculumItemResponse> types,
	boolean needsReview
) { }
