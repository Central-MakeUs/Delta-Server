package cmc.delta.domain.problem.application.validation.query;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.application.exception.ProblemValidationException;
import cmc.delta.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.mockito.Mockito.*;

class ProblemListRequestValidatorTest {

	private final ProblemListRequestValidator validator = new ProblemListRequestValidator();

	@Test
	@DisplayName("페이지네이션 검증: page가 음수면 INVALID_REQUEST")
	void validatePagination_whenPageNegative_thenThrowsInvalidRequest() {
		// given
		Pageable pageable = mock(Pageable.class);
		when(pageable.getPageNumber()).thenReturn(-1);
		when(pageable.getPageSize()).thenReturn(10);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validatePagination(pageable),
			ProblemValidationException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("페이지네이션 검증: size가 1 미만이면 PROBLEM_LIST_INVALID_PAGINATION")
	void validatePagination_whenSizeLessThan1_thenThrowsInvalidPagination() {
		// given
		Pageable pageable = mock(Pageable.class);
		when(pageable.getPageNumber()).thenReturn(0);
		when(pageable.getPageSize()).thenReturn(0);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validatePagination(pageable),
			ProblemValidationException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_LIST_INVALID_PAGINATION);
	}

	@Test
	@DisplayName("페이지네이션 검증: size가 50 초과면 PROBLEM_LIST_INVALID_PAGINATION")
	void validatePagination_whenSizeTooLarge_thenThrowsInvalidPagination() {
		// given
		Pageable pageable = mock(Pageable.class);
		when(pageable.getPageNumber()).thenReturn(0);
		when(pageable.getPageSize()).thenReturn(51);

		// when
		ProblemValidationException ex = catchThrowableOfType(
			() -> validator.validatePagination(pageable),
			ProblemValidationException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_LIST_INVALID_PAGINATION);
	}

	@Test
	@DisplayName("페이지네이션 검증: page/size가 유효하면 통과")
	void validatePagination_whenValid_thenOk() {
		// given
		Pageable pageable = mock(Pageable.class);
		when(pageable.getPageNumber()).thenReturn(0);
		when(pageable.getPageSize()).thenReturn(10);

		// when/then
		assertThatCode(() -> validator.validatePagination(pageable))
			.doesNotThrowAnyException();
	}
}
