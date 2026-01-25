package cmc.delta.domain.problem.application.command;

public record ProblemUpdateCommand(
	Integer answerChoiceNo,
	String answerValue,
	String solutionText,
	boolean hasAnswerChange,
	boolean hasSolutionChange) {
}
