package cmc.delta.domain.problem.application.port.in.scan.result;

import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import java.util.List;

public record ProblemScanSummaryClassificationResponse(
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	List<CurriculumItemResponse> types,
	boolean needsReview) {
}
