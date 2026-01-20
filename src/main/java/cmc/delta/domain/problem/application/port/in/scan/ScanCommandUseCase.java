package cmc.delta.domain.problem.application.port.in.scan;

import cmc.delta.domain.problem.application.port.in.scan.command.CreateScanCommand;
import cmc.delta.domain.problem.application.port.in.scan.result.ScanCreateResult;

public interface ScanCommandUseCase {
	ScanCreateResult createScan(Long userId, CreateScanCommand command);
	void retryFailed(Long userId, Long scanId);
}
