package cmc.delta.domain.problem.adapter.in.web.scan.dto.response;

public record ProblemScanCreateResponse(
	Long scanId,
	Long assetId,
	String status
) {}
