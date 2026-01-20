package cmc.delta.domain.problem.application.validation.command;

import cmc.delta.domain.problem.application.exception.InvalidProblemCreateRequestException;
import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import org.springframework.stereotype.Component;

@Component
public class ProblemCreateRequestValidator {

	public void validate(CreateWrongAnswerCardCommand command) {
		requireRequestBody(command);
		requireScanId(command.scanId());
		requireFinalUnitId(command.finalUnitId());
		requireFinalTypeId(command.finalTypeId());
		validateAnswerFields(command);
	}

	private void requireRequestBody(CreateWrongAnswerCardCommand command) {
		if (command == null) {
			throw new InvalidProblemCreateRequestException("요청 본문이 비어있습니다.");
		}
	}

	private void requireScanId(Long scanId) {
		if (scanId == null) {
			throw new InvalidProblemCreateRequestException("scanId는 필수입니다.");
		}
	}

	private void requireFinalUnitId(String finalUnitId) {
		if (isBlank(finalUnitId)) {
			throw new InvalidProblemCreateRequestException("finalUnitId는 필수입니다.");
		}
	}

	private void requireFinalTypeId(String finalTypeId) {
		if (isBlank(finalTypeId)) {
			throw new InvalidProblemCreateRequestException("finalTypeId는 필수입니다.");
		}
	}

	private void validateAnswerFields(CreateWrongAnswerCardCommand command) {
		AnswerFormat answerFormat = command.answerFormat();
		requireAnswerFormat(answerFormat);

		if (answerFormat == AnswerFormat.CHOICE) {
			validateChoiceAnswer(command.answerChoiceNo());
			return;
		}

		validateValueAnswer(command.answerValue(), answerFormat);
	}

	private void requireAnswerFormat(AnswerFormat answerFormat) {
		if (answerFormat == null) {
			throw new InvalidProblemCreateRequestException("answerFormat은 필수입니다.");
		}
	}

	private void validateChoiceAnswer(Integer answerChoiceNo) {
		if (answerChoiceNo == null) {
			throw new InvalidProblemCreateRequestException("객관식은 answerChoiceNo가 필수입니다.");
		}
		if (answerChoiceNo.intValue() < 1) {
			throw new InvalidProblemCreateRequestException("answerChoiceNo는 1 이상이어야 합니다.");
		}
	}

	private void validateValueAnswer(String answerValue, AnswerFormat answerFormat) {
		if (isBlank(answerValue)) {
			throw new InvalidProblemCreateRequestException(
				"정답 값(answerValue)은 필수입니다. (answerFormat=" + answerFormat.name() + ")"
			);
		}
	}

	private boolean isBlank(String value) {
		if (value == null) return true;
		return value.trim().isEmpty();
	}
}
