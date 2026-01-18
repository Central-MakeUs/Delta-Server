package cmc.delta.domain.problem.persistence.scan.query;

import cmc.delta.domain.problem.persistence.scan.query.dto.ProblemScanListRow;
import java.util.Optional;

public interface ProblemScanQueryRepository {
	Optional<ProblemScanListRow> findSummaryRow(Long userId, Long scanId);
}
