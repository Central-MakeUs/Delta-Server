package cmc.delta.domain.problem.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemUpdateInvalidAnswerException extends BusinessException {
	public ProblemUpdateInvalidAnswerException() {
		super(ErrorCode.PROBLEM_UPDATE_INVALID_ANSWER);
	}
}
