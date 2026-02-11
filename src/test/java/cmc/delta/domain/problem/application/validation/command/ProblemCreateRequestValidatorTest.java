package cmc.delta.domain.problem.application.validation.command;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.exception.ProblemValidationException;
import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import cmc.delta.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemCreateRequestValidatorTest {

	private final ProblemCreateRequestValidator validator = new ProblemCreateRequestValidator();

	@Test
	@DisplayName("오답노트 생성 검증: command가 null이면 INVALID_REQUEST")
	void validate_whenCommandNull_thenThrowsInvalidRequest() {
		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validate(null),
			ProblemValidationException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("오답노트 생성 검증: scanId가 null이면 INVALID_REQUEST")
	void validate_whenScanIdNull_thenThrowsInvalidRequest() {
		// given
		CreateWrongAnswerCardCommand cmd = new CreateWrongAnswerCardCommand(
			null,
			"U1",
			java.util.List.of("T1"),
			AnswerFormat.TEXT,
			null,
			"ans",
			null);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validate(cmd),
			ProblemValidationException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("오답노트 생성 검증: finalUnitId가 blank면 INVALID_REQUEST")
	void validate_whenFinalUnitIdBlank_thenThrowsInvalidRequest() {
		// given
		CreateWrongAnswerCardCommand cmd = new CreateWrongAnswerCardCommand(
			1L,
			"  ",
			java.util.List.of("T1"),
			AnswerFormat.TEXT,
			null,
			"ans",
			null);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validate(cmd),
			ProblemValidationException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("오답노트 생성 검증: finalTypeIds가 empty면 INVALID_REQUEST")
	void validate_whenFinalTypeIdsEmpty_thenThrowsInvalidRequest() {
		// given
		CreateWrongAnswerCardCommand cmd = new CreateWrongAnswerCardCommand(
			1L,
			"U1",
			java.util.List.of(),
			AnswerFormat.TEXT,
			null,
			"ans",
			null);

		// when
		ProblemException ex = catchThrowableOfType(
			() -> validator.validate(cmd),
			ProblemException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("오답노트 생성 검증: finalTypeIds가 전부 blank면 INVALID_REQUEST")
	void validate_whenFinalTypeIdsAllBlank_thenThrowsInvalidRequest() {
		// given
		CreateWrongAnswerCardCommand cmd = new CreateWrongAnswerCardCommand(
			1L,
			"U1",
			java.util.Arrays.asList(" ", null),
			AnswerFormat.TEXT,
			null,
			"ans",
			null);

		// when
		ProblemException ex = catchThrowableOfType(
			() -> validator.validate(cmd),
			ProblemException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("오답노트 생성 검증: answerFormat이 null이면 INVALID_REQUEST")
	void validate_whenAnswerFormatNull_thenThrowsInvalidRequest() {
		// given
		CreateWrongAnswerCardCommand cmd = new CreateWrongAnswerCardCommand(
			1L,
			"U1",
			java.util.List.of("T1"),
			null,
			null,
			"ans",
			null);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validate(cmd),
			ProblemValidationException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("오답노트 생성 검증: 객관식인데 answerChoiceNo가 null이면 INVALID_REQUEST")
	void validate_whenChoiceAndAnswerChoiceNoNull_thenThrowsInvalidRequest() {
		// given
		CreateWrongAnswerCardCommand cmd = new CreateWrongAnswerCardCommand(
			1L,
			"U1",
			java.util.List.of("T1"),
			AnswerFormat.CHOICE,
			null,
			null,
			null);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validate(cmd),
			ProblemValidationException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("오답노트 생성 검증: 객관식인데 answerChoiceNo가 1 미만이면 INVALID_REQUEST")
	void validate_whenChoiceAndAnswerChoiceNoLessThan1_thenThrowsInvalidRequest() {
		// given
		CreateWrongAnswerCardCommand cmd = new CreateWrongAnswerCardCommand(
			1L,
			"U1",
			java.util.List.of("T1"),
			AnswerFormat.CHOICE,
			0,
			null,
			null);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validate(cmd),
			ProblemValidationException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("오답노트 생성 검증: 주관식인데 answerValue가 blank여도 예외가 발생하지 않음")
	void validate_whenTextAndAnswerValueBlank_thenDoesNotThrow() {
		// given
		CreateWrongAnswerCardCommand cmd = new CreateWrongAnswerCardCommand(
			1L,
			"U1",
			java.util.List.of("T1"),
			AnswerFormat.TEXT,
			null,
			"  ",
			null);

		// when/then
		assertThatCode(() -> validator.validate(cmd)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("오답노트 생성 검증: 주관식인데 answerValue가 null이면 예외가 발생하지 않음")
	void validate_whenTextAndAnswerValueNull_thenDoesNotThrow() {
		// given
		CreateWrongAnswerCardCommand cmd = new CreateWrongAnswerCardCommand(
			1L,
			"U1",
			java.util.List.of("T1"),
			AnswerFormat.TEXT,
			null,
			null,
			null);

		// when/then
		assertThatCode(() -> validator.validate(cmd)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("오답노트 생성 검증: 조건이 유효하면 예외가 발생하지 않음")
	void validate_whenValid_thenDoesNotThrow() {
		// given
		CreateWrongAnswerCardCommand choice = new CreateWrongAnswerCardCommand(
			1L,
			"U1",
			java.util.List.of("T1"),
			AnswerFormat.CHOICE,
			1,
			null,
			null);

		CreateWrongAnswerCardCommand text = new CreateWrongAnswerCardCommand(
			1L,
			"U1",
			java.util.List.of("T1"),
			AnswerFormat.TEXT,
			null,
			"ans",
			null);

		// when/then
		assertThatCode(() -> validator.validate(choice)).doesNotThrowAnyException();
		assertThatCode(() -> validator.validate(text)).doesNotThrowAnyException();
	}
}
