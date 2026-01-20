package cmc.delta.domain.problem.application.port.in.scan;

import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanDetailResponse;
import cmc.delta.domain.problem.adapter.in.web.scan.dto.response.ProblemScanSummaryResponse;

public interface ProblemScanQueryUseCase {
	ProblemScanDetailResponse getDetail(Long userId, Long scanId);
	ProblemScanSummaryResponse getSummary(Long userId, Long scanId);
}
