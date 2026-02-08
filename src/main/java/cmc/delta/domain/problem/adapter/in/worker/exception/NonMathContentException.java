package cmc.delta.domain.problem.adapter.in.worker.exception;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.global.error.ErrorCode;

public class NonMathContentException extends ProblemScanWorkerException {

	public NonMathContentException(Long scanId) {
		super(ErrorCode.OCR_PROCESSING_FAILED, FailureReason.OCR_NOT_MATH, scanMessage(scanId));
	}
}
