package cmc.delta.domain.problem.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemException extends BusinessException {

	public ProblemException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ProblemException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public static ProblemException scanNotFound() {
		return new ProblemException(ErrorCode.PROBLEM_SCAN_NOT_FOUND);
	}

	public static ProblemException originalAssetNotFound() {
		return new ProblemException(ErrorCode.PROBLEM_ASSET_NOT_FOUND);
	}
}
