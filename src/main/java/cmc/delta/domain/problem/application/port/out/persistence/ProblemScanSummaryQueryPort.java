package cmc.delta.domain.problem.application.port.out.persistence;

import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanListRow;
import java.util.Optional;

public interface ProblemScanSummaryQueryPort {
	Optional<ScanListRow> findSummaryRow(Long userId, Long scanId);
}
