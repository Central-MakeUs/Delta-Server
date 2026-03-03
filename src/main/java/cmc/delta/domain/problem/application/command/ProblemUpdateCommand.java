package cmc.delta.domain.problem.application.command;

import cmc.delta.domain.problem.model.enums.AnswerFormat;

public record ProblemUpdateCommand(
	Integer answerChoiceNo,
	String answerValue,
	AnswerFormat answerFormat,
	String memoText,
	boolean hasAnswerChange,
	boolean hasAnswerFormatChange,
	boolean hasMemoChange) {

	public boolean hasNoUpdates() {
		return !hasAnswerChange && !hasMemoChange;
	}
}
