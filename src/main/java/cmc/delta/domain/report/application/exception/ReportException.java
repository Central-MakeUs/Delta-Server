package cmc.delta.domain.report.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class ReportException extends BusinessException {

	public ReportException(ErrorCode errorCode) {
		super(errorCode);
	}

	public static ReportException notEnoughProblems() {
		return new ReportException(ErrorCode.REPORT_NOT_ENOUGH_PROBLEMS);
	}

	public static ReportException notFound() {
		return new ReportException(ErrorCode.REPORT_NOT_FOUND);
	}
}
