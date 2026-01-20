package cmc.delta.domain.problem.application.port.out.scan.query;

import cmc.delta.domain.problem.adapter.out.persistence.scan.query.dto.ScanListRow;
import cmc.delta.domain.problem.adapter.out.persistence.scan.query.projection.ScanDetailProjection;
import java.util.Optional;

public interface ScanQueryPort {

	Optional<ScanListRow> findListRow(Long userId, Long scanId);

	Optional<ScanDetailProjection> findDetail(Long userId, Long scanId);
}
