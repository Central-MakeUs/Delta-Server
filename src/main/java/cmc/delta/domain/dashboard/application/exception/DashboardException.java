package cmc.delta.domain.dashboard.application.exception;

import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;

public class DashboardException extends BusinessException {

	public DashboardException(ErrorCode errorCode) {
		super(errorCode);
	}
}
