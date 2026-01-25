package cmc.delta.domain.curriculum.application.port.in.type.result;

public record ProblemTypeItemResponse(
	String id,
	String name,
	boolean custom,
	boolean active,
	int sortOrder
) {}
