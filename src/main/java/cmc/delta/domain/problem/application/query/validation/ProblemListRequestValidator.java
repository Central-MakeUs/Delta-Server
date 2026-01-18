package cmc.delta.domain.problem.application.query.validation;

import cmc.delta.domain.problem.application.query.exception.InvalidProblemListPaginationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ProblemListRequestValidator {

	private static final int MAX_SIZE = 50;

	public void validatePagination(Pageable pageable) {
		int page = pageable.getPageNumber();
		int size = pageable.getPageSize();

		if (page < 0) {
			throw new InvalidProblemListPaginationException();
		}
		if (size < 1 || size > MAX_SIZE) {
			throw new InvalidProblemListPaginationException();
		}
	}
}
