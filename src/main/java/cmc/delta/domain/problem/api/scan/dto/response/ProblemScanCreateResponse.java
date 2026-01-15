package cmc.delta.domain.problem.api.scan.dto.response;

public record ProblemScanCreateResponse(
	Long scanId,
	Long assetId,
	String status
) {}
