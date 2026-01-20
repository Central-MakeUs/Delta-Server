package cmc.delta.domain.problem.adapter.out.scan.adapter;

import cmc.delta.domain.problem.application.port.out.persistence.ProblemScanSummaryQueryPort;
import cmc.delta.domain.problem.adapter.out.persistence.scan.query.ScanListRepository;
import cmc.delta.domain.problem.adapter.out.persistence.scan.query.dto.ScanListRow;
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
