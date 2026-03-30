package cmc.delta.domain.problem.application.port.in.scan;

import cmc.delta.domain.problem.application.port.in.scan.command.CreateScanGroupCommand;
import cmc.delta.domain.problem.application.port.in.scan.result.ScanGroupCreateResult;

public interface ScanGroupCommandUseCase {

	ScanGroupCreateResult createScanGroup(Long userId, CreateScanGroupCommand command);
}
