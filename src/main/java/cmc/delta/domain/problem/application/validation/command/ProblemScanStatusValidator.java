package cmc.delta.domain.problem.application.validation.command;

import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class ProblemScanStatusValidator {

	private static final String MSG_RETRY_ONLY_FAILED = "FAILED 상태에서만 재시도할 수 있습니다.";

	public void requireFailed(ProblemScan scan) {
		if (scan.getStatus() != ScanStatus.FAILED) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, MSG_RETRY_ONLY_FAILED);
		}
	}
}
