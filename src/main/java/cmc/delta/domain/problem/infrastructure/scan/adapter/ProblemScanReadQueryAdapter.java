package cmc.delta.domain.problem.infrastructure.scan.adapter;

import cmc.delta.domain.problem.application.query.port.ProblemScanSummaryQueryPort;
import cmc.delta.domain.problem.persistence.scan.query.ScanListRepository;
import cmc.delta.domain.problem.persistence.scan.query.dto.ScanListRow;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemScanReadQueryAdapter implements ProblemScanSummaryQueryPort {

	private final ScanListRepository scanListRepository;

	@Override
	public Optional<ScanListRow> findSummaryRow(Long userId, Long scanId) {
		return scanListRepository.findListRow(userId, scanId);
	}
}
