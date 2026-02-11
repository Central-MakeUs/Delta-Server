package cmc.delta.domain.problem.application.command;

public record ProblemUpdateCommand(
	Integer answerChoiceNo,
	String answerValue,
	String memoText,
	boolean hasAnswerChange,
	boolean hasMemoChange) {

	public boolean hasNoUpdates() {
		return !hasAnswerChange && !hasMemoChange;
	}
}
