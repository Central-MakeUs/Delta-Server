package cmc.delta.domain.problem.adapter.in.worker.exception;

import cmc.delta.domain.problem.adapter.in.worker.support.failure.FailureReason;
import cmc.delta.global.error.ErrorCode;

public class OcrTextTooShortException extends ProblemScanWorkerException {

	public OcrTextTooShortException(Long scanId) {
		super(ErrorCode.OCR_PROCESSING_FAILED, FailureReason.OCR_TEXT_TOO_SHORT, scanMessage(scanId));
	}
}
