package cmc.delta.domain.problem.application.port.in.scan;

import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanGroupSummaryResponse;

public interface ScanGroupQueryUseCase {

	ProblemScanGroupSummaryResponse getGroupSummary(Long userId, Long groupId);
}
