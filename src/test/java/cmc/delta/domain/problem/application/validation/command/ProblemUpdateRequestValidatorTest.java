package cmc.delta.domain.problem.application.validation.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.command.ProblemUpdateCommand;
import cmc.delta.domain.problem.application.exception.ProblemValidationException;
import cmc.delta.domain.problem.application.port.in.problem.command.UpdateWrongAnswerCardCommand;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemUpdateRequestValidatorTest {

	private final ProblemUpdateRequestValidator validator = new ProblemUpdateRequestValidator();

	@Test
	@DisplayName("오답노트 수정 검증: 답/해설 둘 다 변경 없으면 PROBLEM_UPDATE_EMPTY")
	void validateAndNormalize_whenEmpty_thenThrowsEmpty() {
		// given
		Problem p = problemWithFormat(AnswerFormat.TEXT);
		UpdateWrongAnswerCardCommand cmd = new UpdateWrongAnswerCardCommand(null, null, null);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validateAndNormalize(p, cmd),
			ProblemValidationException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_UPDATE_EMPTY);
	}

	@Test
	@DisplayName("오답노트 수정 검증: 객관식인데 answerChoiceNo 없이 answer 변경 시 PROBLEM_UPDATE_INVALID_ANSWER")
	void validateAndNormalize_whenChoiceAndMissingChoiceNo_thenThrowsInvalidAnswer() {
		// given
		Problem p = problemWithFormat(AnswerFormat.CHOICE);
		UpdateWrongAnswerCardCommand cmd = new UpdateWrongAnswerCardCommand(null, "ignored", null);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validateAndNormalize(p, cmd),
			ProblemValidationException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_UPDATE_INVALID_ANSWER);
	}

	@Test
	@DisplayName("오답노트 수정 검증: 객관식이면 answerChoiceNo만 채우고 answerValue는 null로 정리")
	void validateAndNormalize_whenChoice_thenSetsChoiceNoAndNullValue() {
		// given
		Problem p = problemWithFormat(AnswerFormat.CHOICE);
		UpdateWrongAnswerCardCommand cmd = new UpdateWrongAnswerCardCommand(3, "shouldDrop", null);

		// when
		ProblemUpdateCommand out = validator.validateAndNormalize(p, cmd);

		// then
		assertThat(out.hasAnswerChange()).isTrue();
		assertThat(out.answerChoiceNo()).isEqualTo(3);
		assertThat(out.answerValue()).isNull();
		assertThat(out.hasSolutionChange()).isFalse();
	}

	@Test
	@DisplayName("오답노트 수정 검증: 주관식이면 answerValue를 trimToNull 처리하고 choiceNo는 null")
	void validateAndNormalize_whenText_thenTrimsAnswerValue() {
		// given
		Problem p = problemWithFormat(AnswerFormat.TEXT);
		UpdateWrongAnswerCardCommand cmd = new UpdateWrongAnswerCardCommand(1, "  ans  ", null);

		// when
		ProblemUpdateCommand out = validator.validateAndNormalize(p, cmd);

		// then
		assertThat(out.hasAnswerChange()).isTrue();
		assertThat(out.answerChoiceNo()).isNull();
		assertThat(out.answerValue()).isEqualTo("ans");
	}

	@Test
	@DisplayName("오답노트 수정 검증: solutionText는 trimToNull 처리")
	void validateAndNormalize_whenSolutionText_thenTrimsSolution() {
		// given
		Problem p = problemWithFormat(AnswerFormat.TEXT);
		UpdateWrongAnswerCardCommand cmd = new UpdateWrongAnswerCardCommand(null, null, "  ");

		// when
		ProblemUpdateCommand out = validator.validateAndNormalize(p, cmd);

		// then
		assertThat(out.hasSolutionChange()).isTrue();
		assertThat(out.solutionText()).isNull();
		assertThat(out.hasAnswerChange()).isFalse();
	}

	private Problem problemWithFormat(AnswerFormat format) {
		User user = mock(User.class);
		ProblemScan scan = mock(ProblemScan.class);
		Unit unit = mock(Unit.class);
		ProblemType type = mock(ProblemType.class);
		return Problem.create(user, scan, unit, type, RenderMode.LATEX, "md", format, "a", 1, "s");
	}
}
