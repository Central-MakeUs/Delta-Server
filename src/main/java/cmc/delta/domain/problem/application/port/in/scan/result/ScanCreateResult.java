package cmc.delta.domain.problem.application.port.in.scan.result;

public record ScanCreateResult(
	Long scanId,
	Long assetId,
	String status) {
}
