package cmc.delta.domain.problem.adapter.out.persistence.scan;

import cmc.delta.domain.problem.application.port.out.scan.ProblemScanGroupRepositoryPort;
import cmc.delta.domain.problem.model.scan.ProblemScanGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemScanGroupRepositoryAdapter implements ProblemScanGroupRepositoryPort {

	private final ScanGroupRepository scanGroupRepository;

	@Override
	public ProblemScanGroup save(ProblemScanGroup group) {
		return scanGroupRepository.save(group);
	}
}
