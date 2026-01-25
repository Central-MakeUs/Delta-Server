package cmc.delta.domain.problem.application.port.in.scan;

import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanDetailResponse;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanSummaryResponse;

public interface ProblemScanQueryUseCase {
	ProblemScanDetailResponse getDetail(Long userId, Long scanId);

	ProblemScanSummaryResponse getSummary(Long userId, Long scanId);
}
