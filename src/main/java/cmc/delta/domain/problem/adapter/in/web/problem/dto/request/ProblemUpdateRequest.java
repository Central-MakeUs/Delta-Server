package cmc.delta.domain.problem.adapter.in.web.problem.dto.request;

public record ProblemUpdateRequest(
	Integer answerChoiceNo,
	String answerValue,
	String solutionText
) {
}
