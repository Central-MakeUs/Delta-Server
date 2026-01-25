package cmc.delta.domain.problem.application.validation.query;

import cmc.delta.domain.problem.application.exception.ProblemValidationException;
import cmc.delta.domain.problem.application.port.in.support.PageQuery;
import cmc.delta.global.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ProblemListRequestValidator {

	private static final int MAX_SIZE = 50;

	public void validatePagination(PageQuery pageQuery) {
		int page = pageQuery.page();
		int size = pageQuery.size();

		if (page < 0) {
			throw new ProblemValidationException(ErrorCode.INVALID_REQUEST);
		}
		if (size < 1 || size > MAX_SIZE) {
			throw new ProblemValidationException(ErrorCode.PROBLEM_LIST_INVALID_PAGINATION);

		}
	}
}
