package cmc.delta.domain.problem.application.query;

import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanDetailResponse;
import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanSummaryResponse;

public interface ProblemScanQueryService {
	ProblemScanDetailResponse getDetail(Long userId, Long scanId);
	ProblemScanSummaryResponse getSummary(Long userId, Long scanId);
}
