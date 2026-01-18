package cmc.delta.domain.problem.application.monitor;

import cmc.delta.domain.problem.application.monitor.dto.WorkerMetricsSnapshot;
import cmc.delta.domain.problem.persistence.scan.ScanRepository;
import java.time.Clock;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemScanMetricsService {

	private final Clock clock;
	private final ScanRepository scanRepository;

	@Transactional(readOnly = true)
	public WorkerMetricsSnapshot lastMinutes(
		long windowMinutes,
		long ocrLockLeaseSeconds,
		long aiLockLeaseSeconds
	) {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime from = now.minusMinutes(windowMinutes);

		long ocrDoneCount = scanRepository.countByOcrCompletedAtGreaterThanEqual(from);
		long aiDoneCount = scanRepository.countByAiCompletedAtGreaterThanEqual(from);

		LocalDateTime ocrStaleBefore = now.minusSeconds(ocrLockLeaseSeconds);
		LocalDateTime aiStaleBefore = now.minusSeconds(aiLockLeaseSeconds);

		long failedCount = scanRepository.countFailedSince(from);

		long ocrBacklog = scanRepository.countOcrBacklog(now, ocrStaleBefore);
		long aiBacklog = scanRepository.countAiBacklog(now, aiStaleBefore);

		return new WorkerMetricsSnapshot(
			windowMinutes,
			from,
			now,
			ocrDoneCount,
			aiDoneCount,
			failedCount,
			ocrBacklog,
			aiBacklog
		);
	}
}
