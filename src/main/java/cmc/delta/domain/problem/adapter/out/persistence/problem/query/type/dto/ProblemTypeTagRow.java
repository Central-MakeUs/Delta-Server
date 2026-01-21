package cmc.delta.domain.problem.adapter.out.persistence.problem.query.type.dto;

public record ProblemTypeTagRow(
	Long problemId,
	String typeId,
	String typeName
) {}
