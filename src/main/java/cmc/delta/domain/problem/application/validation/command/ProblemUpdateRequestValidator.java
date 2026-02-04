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

	private static final String EMPTY = "";

	public ProblemUpdateCommand validateAndNormalize(Problem problem, UpdateWrongAnswerCardCommand command) {
		UpdateChangeFlags flags = readFlags(command);
		if (flags.hasNoChanges()) {
			throw new ProblemValidationException(ErrorCode.PROBLEM_UPDATE_EMPTY);
		}

		AnswerUpdate answerUpdate = resolveAnswerUpdate(problem, command, flags);
		String normalizedSolutionText = normalizeSolutionText(command.solutionText());

		return new ProblemUpdateCommand(
			answerUpdate.answerChoiceNo(),
			answerUpdate.answerValue(),
			normalizedSolutionText,
			flags.hasAnswerChange(),
			flags.hasSolutionChange());
	}

	private UpdateChangeFlags readFlags(UpdateWrongAnswerCardCommand command) {
		boolean hasAnswerChange = command.answerChoiceNo() != null || command.answerValue() != null;
		boolean hasSolutionChange = command.solutionText() != null;
		return new UpdateChangeFlags(hasAnswerChange, hasSolutionChange);
	}

	private AnswerUpdate resolveAnswerUpdate(
		Problem problem,
		UpdateWrongAnswerCardCommand command,
		UpdateChangeFlags flags) {
		if (!flags.hasAnswerChange()) {
			return AnswerUpdate.empty();
		}
		AnswerFormat format = problem.getAnswerFormat();
		if (format == AnswerFormat.CHOICE) {
			return resolveChoiceAnswer(command);
		}
		String answerValue = normalizeAnswerValue(command.answerValue());
		return new AnswerUpdate(null, answerValue);
	}

	private AnswerUpdate resolveChoiceAnswer(UpdateWrongAnswerCardCommand command) {
		if (command.answerChoiceNo() == null) {
			throw new ProblemValidationException(ErrorCode.PROBLEM_UPDATE_INVALID_ANSWER);
		}
		return new AnswerUpdate(command.answerChoiceNo(), null);
	}

	private String normalizeAnswerValue(String answerValue) {
		return trimToNull(answerValue);
	}

	private String normalizeSolutionText(String solutionText) {
		return trimToNull(solutionText);
	}

	private String trimToNull(String v) {
		if (v == null) {
			return null;
		}
		String trimmed = v.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record UpdateChangeFlags(boolean hasAnswerChange, boolean hasSolutionChange) {
		private boolean hasNoChanges() {
			return !hasAnswerChange && !hasSolutionChange;
		}
	}

	private record AnswerUpdate(Integer answerChoiceNo, String answerValue) {
		private static AnswerUpdate empty() {
			return new AnswerUpdate(null, null);
		}
	}
}
