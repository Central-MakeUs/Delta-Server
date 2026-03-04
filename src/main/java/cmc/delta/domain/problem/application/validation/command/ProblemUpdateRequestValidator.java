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
		UpdateChangeFlags flags = readFlags(problem, command);
		if (flags.hasNoChanges()) {
			throw new ProblemValidationException(ErrorCode.PROBLEM_UPDATE_EMPTY);
		}

		AnswerFormat targetAnswerFormat = resolveTargetAnswerFormat(problem, command);
		AnswerUpdate answerUpdate = resolveAnswerUpdate(command, flags, targetAnswerFormat);
		String normalizedMemoText = normalizeMemoText(command.memoText());

		return new ProblemUpdateCommand(
			answerUpdate.answerChoiceNo(),
			answerUpdate.answerValue(),
			targetAnswerFormat,
			normalizedMemoText,
			flags.hasAnswerChange(),
			flags.hasAnswerFormatChange(),
			flags.hasMemoChange());
	}

	private UpdateChangeFlags readFlags(Problem problem, UpdateWrongAnswerCardCommand command) {
		boolean hasAnswerFormatChange = command.answerFormat() != null
			&& command.answerFormat() != problem.getAnswerFormat();
		boolean hasAnswerValueChange = command.answerChoiceNo() != null || command.answerValue() != null;
		boolean hasAnswerChange = hasAnswerFormatChange || hasAnswerValueChange;
		boolean hasMemoChange = command.memoText() != null;
		return new UpdateChangeFlags(hasAnswerChange, hasAnswerFormatChange, hasMemoChange);
	}

	private AnswerFormat resolveTargetAnswerFormat(Problem problem, UpdateWrongAnswerCardCommand command) {
		if (command.answerFormat() == null) {
			return problem.getAnswerFormat();
		}
		return command.answerFormat();
	}

	private AnswerUpdate resolveAnswerUpdate(
		UpdateWrongAnswerCardCommand command,
		UpdateChangeFlags flags,
		AnswerFormat targetAnswerFormat) {
		if (!flags.hasAnswerChange()) {
			return AnswerUpdate.empty();
		}
		if (targetAnswerFormat == AnswerFormat.CHOICE) {
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

	private String normalizeMemoText(String memoText) {
		return trimToNull(memoText);
	}

	private String trimToNull(String v) {
		if (v == null) {
			return null;
		}
		String trimmed = v.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record UpdateChangeFlags(boolean hasAnswerChange, boolean hasAnswerFormatChange, boolean hasMemoChange) {
		private boolean hasNoChanges() {
			return !hasAnswerChange && !hasMemoChange;
		}
	}

	private record AnswerUpdate(Integer answerChoiceNo, String answerValue) {
		private static AnswerUpdate empty() {
			return new AnswerUpdate(null, null);
		}
	}
}
