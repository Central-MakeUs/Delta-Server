package cmc.delta.domain.problem.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ProblemAssetNotFoundException extends BusinessException {
	public ProblemAssetNotFoundException() {
		super(ErrorCode.PROBLEM_ASSET_NOT_FOUND);
	}
}
