package cmc.delta.domain.problem.application.port.in.scan.result;

import java.util.List;

public record ProblemScanGroupCreateResponse(Long scanGroupId, List<ProblemScanCreateResponse> scans) {

	public static ProblemScanGroupCreateResponse from(ScanGroupCreateResult result) {
		List<ProblemScanCreateResponse> scans = result.scans().stream()
			.map(ProblemScanCreateResponse::from)
			.toList();
		return new ProblemScanGroupCreateResponse(result.scanGroupId(), scans);
	}
}
