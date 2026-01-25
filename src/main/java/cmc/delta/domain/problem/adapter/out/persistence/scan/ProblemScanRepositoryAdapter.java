package cmc.delta.domain.problem.adapter.out.persistence.scan;

import cmc.delta.domain.problem.application.port.out.scan.ProblemScanRepositoryPort;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemScanRepositoryAdapter implements ProblemScanRepositoryPort {

	private final ScanRepository scanRepository;

	@Override
	public ProblemScan save(ProblemScan scan) {
		return scanRepository.save(scan);
	}

	@Override
	public Optional<ProblemScan> findOwnedById(Long scanId, Long userId) {
		return scanRepository.findByIdAndUserId(scanId, userId);
	}

	@Override
	public Optional<ProblemScan> findOwnedByForUpdate(Long scanId, Long userId) {
		return scanRepository.findOwnedByForUpdate(scanId, userId);
	}
}
