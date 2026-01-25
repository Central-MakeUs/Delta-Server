package cmc.delta.domain.problem.application.validation.command;

import cmc.delta.domain.problem.application.command.ProblemUpdateCommand;
import cmc.delta.domain.problem.application.exception.ProblemValidationException;
import cmc.delta.domain.problem.application.port.in.problem.command.UpdateWrongAnswerCardCommand;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.global.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ProblemUpdateRequestValidator {

	public ProblemUpdateCommand validateAndNormalize(Problem problem, UpdateWrongAnswerCardCommand command) {
		boolean hasAnswerChange = command.answerChoiceNo() != null || command.answerValue() != null;
		boolean hasSolutionChange = command.solutionText() != null;

		if (!hasAnswerChange && !hasSolutionChange) {
			throw new ProblemValidationException(ErrorCode.PROBLEM_UPDATE_EMPTY);
		}

		String normalizedAnswerValue = trimToNull(command.answerValue());
		String normalizedSolutionText = trimToNull(command.solutionText());

		Integer answerChoiceNo = null;
		String answerValue = null;

		if (hasAnswerChange) {
			AnswerFormat format = problem.getAnswerFormat();

			if (format == AnswerFormat.CHOICE) {
				if (command.answerChoiceNo() == null) {
					throw new ProblemValidationException(ErrorCode.PROBLEM_UPDATE_INVALID_ANSWER);
				}
				answerChoiceNo = command.answerChoiceNo();
				answerValue = null;
			} else {
				answerChoiceNo = null;
				answerValue = normalizedAnswerValue;
				// 원하면 여기서 normalizedAnswerValue == null 이면 예외로 막아도 됨
			}
		}

		return new ProblemUpdateCommand(
			answerChoiceNo,
			answerValue,
			normalizedSolutionText,
			hasAnswerChange,
			hasSolutionChange);
	}

	private String trimToNull(String v) {
		if (v == null)
			return null;
		String t = v.trim();
		return t.isEmpty() ? null : t;
	}
}
