package cmc.delta.domain.problem.adapter.in.web.problem.dto.request;

import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import java.util.List;

public record ProblemCreateRequest(
	Long scanId,
	String finalUnitId,
	List<String> finalTypeIds,
	AnswerFormat answerFormat,
	Integer answerChoiceNo,
	String answerValue,
	String memoText) {
	public CreateWrongAnswerCardCommand toCommand() {
		return new CreateWrongAnswerCardCommand(
			scanId,
			finalUnitId,
			finalTypeIds,
			answerFormat,
			answerChoiceNo,
			answerValue,
			memoText);
	}
}
