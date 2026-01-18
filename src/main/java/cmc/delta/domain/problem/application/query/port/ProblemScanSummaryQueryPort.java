package cmc.delta.domain.problem.application.query.port;

import cmc.delta.domain.problem.persistence.scan.query.dto.ScanListRow;
import java.util.Optional;

public interface ProblemScanSummaryQueryPort {
	Optional<ScanListRow> findSummaryRow(Long userId, Long scanId);
}
