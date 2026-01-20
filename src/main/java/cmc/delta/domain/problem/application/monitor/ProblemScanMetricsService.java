package cmc.delta.domain.problem.application.monitor;

import java.time.Clock;
import java.time.LocalDateTime;

import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemScanMetricsService {

	private final Clock clock;
	private final ScanWorkRepository scanWorkRepository;

	@Transactional(readOnly = true)
	public WorkerMetricsSnapshot lastMinutes(
		long windowMinutes,
		long ocrLockLeaseSeconds,
		long aiLockLeaseSeconds
	) {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime from = now.minusMinutes(windowMinutes);

		long ocrDoneCount = scanWorkRepository.countByOcrCompletedAtGreaterThanEqual(from);
		long aiDoneCount = scanWorkRepository.countByAiCompletedAtGreaterThanEqual(from);

		LocalDateTime ocrStaleBefore = now.minusSeconds(ocrLockLeaseSeconds);
		LocalDateTime aiStaleBefore = now.minusSeconds(aiLockLeaseSeconds);

		long failedCount = scanWorkRepository.countFailedSince(from);

		long ocrBacklog = scanWorkRepository.countOcrBacklog(now, ocrStaleBefore);
		long aiBacklog = scanWorkRepository.countAiBacklog(now, aiStaleBefore);

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
