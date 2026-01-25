package cmc.delta.domain.problem.application.port.in.problem.result;

public record ProblemCreateResponse(
	Long problemId,
	Long scanId) {
}
