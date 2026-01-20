package cmc.delta.domain.problem.adapter.in.web.problem.dto.request;

import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
import cmc.delta.domain.problem.model.enums.AnswerFormat;

public record ProblemCreateRequest(
	Long scanId,
	String finalUnitId,
	String finalTypeId,
	AnswerFormat answerFormat,
	Integer answerChoiceNo,
	String answerValue,
	String solutionText
) {
	public CreateWrongAnswerCardCommand toCommand() {
		return new CreateWrongAnswerCardCommand(
			scanId, finalUnitId, finalTypeId,
			answerFormat, answerChoiceNo, answerValue, solutionText
		);
	}
}
