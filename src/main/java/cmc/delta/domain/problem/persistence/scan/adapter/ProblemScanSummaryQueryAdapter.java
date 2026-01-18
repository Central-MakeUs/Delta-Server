package cmc.delta.domain.problem.persistence.scan.adapter;

import cmc.delta.domain.problem.application.query.port.ProblemScanSummaryQueryPort;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import cmc.delta.domain.problem.persistence.scan.query.dto.ProblemScanListRow;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemScanSummaryQueryAdapter implements ProblemScanSummaryQueryPort {

	private final ProblemScanJpaRepository scanRepository;

	@Override
	public Optional<ProblemScanListRow> findSummaryRow(Long userId, Long scanId) {
		return scanRepository.findSummaryRow(userId, scanId);
	}
}
