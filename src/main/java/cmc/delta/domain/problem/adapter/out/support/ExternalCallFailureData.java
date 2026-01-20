package cmc.delta.domain.problem.adapter.out.support;

public record ExternalCallFailureData(
	String provider,
	String reason,
	Integer httpStatus
) {}
