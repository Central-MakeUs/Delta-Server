package cmc.delta.domain.problem.application.validation.query;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.application.exception.ProblemValidationException;
import cmc.delta.domain.problem.application.port.in.support.PageQuery;
import cmc.delta.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemListRequestValidatorTest {

	private final ProblemListRequestValidator validator = new ProblemListRequestValidator();

	@Test
	@DisplayName("페이지네이션 검증: page가 음수면 INVALID_REQUEST")
	void validatePagination_whenPageNegative_thenThrowsInvalidRequest() {
		// given
		PageQuery pageQuery = new PageQuery(-1, 10);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validatePagination(pageQuery),
			ProblemValidationException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("페이지네이션 검증: size가 1 미만이면 PROBLEM_LIST_INVALID_PAGINATION")
	void validatePagination_whenSizeLessThan1_thenThrowsInvalidPagination() {
		// given
		PageQuery pageQuery = new PageQuery(0, 0);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validatePagination(pageQuery),
			ProblemValidationException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_LIST_INVALID_PAGINATION);
	}

	@Test
	@DisplayName("페이지네이션 검증: size가 50 초과면 PROBLEM_LIST_INVALID_PAGINATION")
	void validatePagination_whenSizeTooLarge_thenThrowsInvalidPagination() {
		// given
		PageQuery pageQuery = new PageQuery(0, 51);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validatePagination(pageQuery),
			ProblemValidationException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_LIST_INVALID_PAGINATION);
	}

	@Test
	@DisplayName("페이지네이션 검증: page/size가 유효하면 통과")
	void validatePagination_whenValid_thenOk() {
		// given
		PageQuery pageQuery = new PageQuery(0, 10);

		// when/then
		assertThatCode(() -> validator.validatePagination(pageQuery))
			.doesNotThrowAnyException();
	}
}
