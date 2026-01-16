package cmc.delta.domain.problem.infrastructure.support;

public record ExternalCallFailureData(
	String provider,
	String reason,
	Integer httpStatus
) {}
