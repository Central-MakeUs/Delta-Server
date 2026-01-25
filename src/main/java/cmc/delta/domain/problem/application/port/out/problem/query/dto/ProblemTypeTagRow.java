package cmc.delta.domain.problem.application.port.out.problem.query.dto;

public record ProblemTypeTagRow(
	Long problemId,
	String typeId,
	String typeName
) {}
