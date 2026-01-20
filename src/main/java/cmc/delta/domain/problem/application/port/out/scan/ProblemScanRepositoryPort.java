package cmc.delta.domain.problem.application.port.out.scan;

import cmc.delta.domain.problem.model.scan.ProblemScan;
import java.util.Optional;

public interface ProblemScanRepositoryPort {
	ProblemScan save(ProblemScan scan);

	Optional<ProblemScan> findOwnedByForUpdate(Long scanId, Long userId);
}
