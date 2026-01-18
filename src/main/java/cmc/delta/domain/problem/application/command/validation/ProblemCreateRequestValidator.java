package cmc.delta.domain.problem.application.command.validation;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemCreateRequest;
import cmc.delta.domain.problem.application.common.exception.InvalidProblemCreateRequestException;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import org.springframework.stereotype.Component;

@Component
public class ProblemCreateRequestValidator {

	public void validate(ProblemCreateRequest request) {
		if (request == null) {
			throw new InvalidProblemCreateRequestException("요청 본문이 비어있습니다.");
		}
		if (request.scanId() == null) {
			throw new InvalidProblemCreateRequestException("scanId는 필수입니다.");
		}
		if (isBlank(request.finalUnitId())) {
			throw new InvalidProblemCreateRequestException("finalUnitId는 필수입니다.");
		}
		if (isBlank(request.finalTypeId())) {
			throw new InvalidProblemCreateRequestException("finalTypeId는 필수입니다.");
		}

		validateAnswer(request);
	}

	private void validateAnswer(ProblemCreateRequest request) {
		AnswerFormat format = request.answerFormat();
		if (format == null) {
			throw new InvalidProblemCreateRequestException("answerFormat은 필수입니다.");
		}

		if (format == AnswerFormat.CHOICE) {
			Integer choiceNo = request.answerChoiceNo();
			if (choiceNo == null) {
				throw new InvalidProblemCreateRequestException("객관식은 answerChoiceNo가 필수입니다.");
			}
			if (choiceNo.intValue() < 1) {
				throw new InvalidProblemCreateRequestException("answerChoiceNo는 1 이상이어야 합니다.");
			}
			return;
		}

		if (format == AnswerFormat.SHORT) {
			if (isBlank(request.answerValue())) {
				throw new InvalidProblemCreateRequestException("주관식은 answerValue가 필수입니다.");
			}
		}
	}

	private boolean isBlank(String value) {
		if (value == null) {
			return true;
		}
		return value.trim().isEmpty();
	}
}
