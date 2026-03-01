package cmc.delta.domain.problem.application.port.in.problem.result;

import java.time.LocalDateTime;

public record ProblemAiSolutionDetailResponse(
	Long taskId,
	String status,
	String failureReason,
	LocalDateTime requestedAt,
	LocalDateTime startedAt,
	LocalDateTime completedAt,
	SolutionContent solution) {

	public static ProblemAiSolutionDetailResponse notRequested() {
		return new ProblemAiSolutionDetailResponse(
			null,
			"NOT_REQUESTED",
			null,
			null,
			null,
			null,
			null);
	}

	public record SolutionContent(
		String plainText) {
	}
}
