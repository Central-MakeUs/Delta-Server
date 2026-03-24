package cmc.delta.domain.problem.application.port.in.scan.result;

import java.util.List;

public record ProblemScanGroupSummaryResponse(
	Long groupId,
	List<ProblemScanSummaryResponse> scans) {
}
