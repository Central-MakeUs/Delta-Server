package cmc.delta.domain.problem.application.worker.exception;

import cmc.delta.domain.problem.application.worker.support.failure.FailureReason;
import cmc.delta.global.error.ErrorCode;

public class OcrTextEmptyException extends ProblemScanWorkerException {

	public OcrTextEmptyException(Long scanId) {
		super(ErrorCode.AI_PROCESSING_FAILED, FailureReason.OCR_TEXT_EMPTY, "scanId=" + scanId);
	}
}
