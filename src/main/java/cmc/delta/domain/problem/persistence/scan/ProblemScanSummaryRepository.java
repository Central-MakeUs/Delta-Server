package cmc.delta.domain.problem.persistence.scan;

import cmc.delta.domain.problem.persistence.scan.dto.ProblemScanSummaryRow;
import java.util.Optional;

public interface ProblemScanSummaryRepository {
	Optional<ProblemScanSummaryRow> findSummaryRow(Long userId, Long scanId);
}
