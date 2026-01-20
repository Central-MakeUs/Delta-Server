package cmc.delta.domain.problem.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemAlreadyCreatedException extends BusinessException {

	public ProblemAlreadyCreatedException() {
		super(ErrorCode.PROBLEM_ALREADY_CREATED);
	}
}
