package cmc.delta.domain.problem.adapter.out.persistence.scan.query;

import cmc.delta.domain.problem.adapter.out.persistence.scan.query.dto.ScanListRow;
import java.util.Optional;

public interface ScanListRepository {
	Optional<ScanListRow> findListRow(Long userId, Long scanId);
}
