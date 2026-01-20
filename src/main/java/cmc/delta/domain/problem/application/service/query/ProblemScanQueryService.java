package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanDetailResponse;
import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanSummaryResponse;

public interface ProblemScanQueryService {
	ProblemScanDetailResponse getDetail(Long userId, Long scanId);
	ProblemScanSummaryResponse getSummary(Long userId, Long scanId);
}
