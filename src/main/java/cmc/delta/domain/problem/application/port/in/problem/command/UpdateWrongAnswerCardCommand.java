package cmc.delta.domain.problem.application.port.in.problem.command;

public record UpdateWrongAnswerCardCommand(
	Integer answerChoiceNo,
	String answerValue,
	String memoText) {
}
