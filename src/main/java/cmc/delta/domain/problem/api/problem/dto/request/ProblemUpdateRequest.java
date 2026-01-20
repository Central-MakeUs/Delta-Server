package cmc.delta.domain.problem.api.problem.dto.request;

public record ProblemUpdateRequest(
	Integer answerChoiceNo,
	String answerValue,
	String solutionText
) {
}
