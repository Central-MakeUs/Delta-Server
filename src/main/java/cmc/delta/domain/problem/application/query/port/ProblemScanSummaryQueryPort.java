package cmc.delta.domain.problem.application.query.port;

import cmc.delta.domain.problem.persistence.scan.dto.ProblemScanSummaryRow;
import java.util.Optional;

public interface ProblemScanSummaryQueryPort {
	Optional<ProblemScanSummaryRow> findSummaryRow(Long userId, Long scanId);
}
