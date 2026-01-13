package cmc.delta.domain.problem.api.dto.response;

public record ProblemScanCreateResponse(
	Long scanId,
	Long assetId,
	String status
) {}
