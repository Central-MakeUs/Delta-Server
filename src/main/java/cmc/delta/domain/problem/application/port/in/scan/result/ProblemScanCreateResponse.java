package cmc.delta.domain.problem.application.port.in.scan.result;

public record ProblemScanCreateResponse(
	Long scanId,
	Long assetId,
	String status) {
	public static ProblemScanCreateResponse from(ScanCreateResult r) {
		return new ProblemScanCreateResponse(r.scanId(), r.assetId(), r.status());
	}
}
