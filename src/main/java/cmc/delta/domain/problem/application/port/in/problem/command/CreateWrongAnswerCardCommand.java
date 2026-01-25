package cmc.delta.domain.problem.application.port.in.problem.command;

import cmc.delta.domain.problem.model.enums.AnswerFormat;
import java.util.List;

public record CreateWrongAnswerCardCommand(
	Long scanId,
	String finalUnitId,
	List<String> finalTypeIds,
	AnswerFormat answerFormat,
	Integer answerChoiceNo,
	String answerValue,
	String solutionText) {
}
