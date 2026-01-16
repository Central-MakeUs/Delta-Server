package cmc.delta.domain.problem.api.scan.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProblemScanSummaryClassificationResponse(
	CurriculumItemResponse subject,
	CurriculumItemResponse unit,
	CurriculumItemResponse type,
	BigDecimal confidence,
	boolean needsReview,
	List<CandidateResponse> unitCandidates,
	List<CandidateResponse> typeCandidates
) { }
