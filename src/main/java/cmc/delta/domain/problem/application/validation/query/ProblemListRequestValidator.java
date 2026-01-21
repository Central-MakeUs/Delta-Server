package cmc.delta.domain.problem.application.validation.query;

import java.util.ArrayList;
import java.util.List;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.exception.ProblemValidationException;
import cmc.delta.global.error.ErrorCode;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ProblemListRequestValidator {

	private static final int MAX_SIZE = 50;

	public void validatePagination(Pageable pageable) {
		int page = pageable.getPageNumber();
		int size = pageable.getPageSize();

		if (page < 0) {
			throw new ProblemValidationException(ErrorCode.INVALID_REQUEST);
		}
		if (size < 1 || size > MAX_SIZE) {
			throw new ProblemValidationException(ErrorCode.PROBLEM_LIST_INVALID_PAGINATION);

		}
	}
}
