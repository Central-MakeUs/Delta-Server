package cmc.delta.domain.problem.application.command.validation;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemUpdateRequest;
import cmc.delta.domain.problem.application.command.ProblemUpdateCommand;
import cmc.delta.domain.problem.application.common.exception.ProblemUpdateEmptyException;
import cmc.delta.domain.problem.application.common.exception.ProblemUpdateInvalidAnswerException;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import cmc.delta.domain.problem.model.problem.Problem;
import org.springframework.stereotype.Component;

@Component
public class ProblemUpdateRequestValidator {

	public ProblemUpdateCommand validateAndNormalize(Problem problem, ProblemUpdateRequest request) {
		boolean hasAnswerChange = request.answerChoiceNo() != null || request.answerValue() != null;
		boolean hasSolutionChange = request.solutionText() != null;

		if (!hasAnswerChange && !hasSolutionChange) {
			throw new ProblemUpdateEmptyException();
		}

		String normalizedAnswerValue = trimToNull(request.answerValue());
		String normalizedSolutionText = trimToNull(request.solutionText());

		Integer answerChoiceNo = null;
		String answerValue = null;

		if (hasAnswerChange) {
			AnswerFormat format = problem.getAnswerFormat();

			if (format == AnswerFormat.CHOICE) {
				if (request.answerChoiceNo() == null) {
					throw new ProblemUpdateInvalidAnswerException();
				}
				answerChoiceNo = request.answerChoiceNo();
				answerValue = null;
			} else {
				answerChoiceNo = null;
				answerValue = normalizedAnswerValue;
			}
		}

		return new ProblemUpdateCommand(
			answerChoiceNo,
			answerValue,
			normalizedSolutionText,
			hasAnswerChange,
			hasSolutionChange
		);
	}

	private String trimToNull(String v) {
		if (v == null) return null;
		String t = v.trim();
		return t.isEmpty() ? null : t;
	}
}
