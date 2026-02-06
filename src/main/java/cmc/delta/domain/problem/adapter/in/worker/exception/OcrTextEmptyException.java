package cmc.delta.domain.problem.adapter.in.worker.exception;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.global.error.ErrorCode;

public class OcrTextEmptyException extends ProblemScanWorkerException {

	public OcrTextEmptyException(Long scanId) {
		super(ErrorCode.OCR_PROCESSING_FAILED, FailureReason.OCR_TEXT_EMPTY, scanMessage(scanId));
	}
}
