package cmc.delta.domain.problem.adapter.in.worker.exception;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.global.error.ErrorCode;

public class OriginalAssetNotFoundException extends ProblemScanWorkerException {

	public OriginalAssetNotFoundException(Long scanId) {
		super(ErrorCode.PROBLEM_ASSET_NOT_FOUND, FailureReason.ASSET_NOT_FOUND, "scanId=" + scanId);
	}
}
