package cmc.delta.domain.problem.application.port.in.problem.result;

import java.time.LocalDateTime;

public record ProblemAiSolutionRequestResponse(
	Long taskId,
	String status,
	boolean reusedExistingTask,
	LocalDateTime requestedAt) {
}
