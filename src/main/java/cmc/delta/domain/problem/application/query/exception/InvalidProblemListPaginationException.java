package cmc.delta.domain.problem.application.query.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class InvalidProblemListPaginationException extends BusinessException {
	public InvalidProblemListPaginationException() {
		super(ErrorCode.PROBLEM_LIST_INVALID_PAGINATION);
	}
}
