package cmc.delta.domain.problem.application.monitor;

import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import java.time.Clock;
import java.time.LocalDateTime;
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
		Window window = Window.lastMinutes(clock, windowMinutes);
		StaleBefore stale = StaleBefore.from(window.now(), ocrLockLeaseSeconds, aiLockLeaseSeconds);

		Counts counts = loadCounts(window, stale);

		return new WorkerMetricsSnapshot(
			windowMinutes,
			window.from(),
			window.now(),
			counts.ocrDone(),
			counts.aiDone(),
			counts.failed(),
			counts.ocrBacklog(),
			counts.aiBacklog()
		);
	}

	private Counts loadCounts(Window window, StaleBefore stale) {
		long ocrDone = scanWorkRepository.countByOcrCompletedAtGreaterThanEqual(window.from());
		long aiDone = scanWorkRepository.countByAiCompletedAtGreaterThanEqual(window.from());
		long failed = scanWorkRepository.countFailedSince(window.from());

		long ocrBacklog = scanWorkRepository.countOcrBacklog(window.now(), stale.ocr());
		long aiBacklog = scanWorkRepository.countAiBacklog(window.now(), stale.ai());

		return new Counts(ocrDone, aiDone, failed, ocrBacklog, aiBacklog);
	}

	private record Window(LocalDateTime from, LocalDateTime now) {
		static Window lastMinutes(Clock clock, long windowMinutes) {
			LocalDateTime now = LocalDateTime.now(clock);
			return new Window(now.minusMinutes(windowMinutes), now);
		}
	}

	private record StaleBefore(LocalDateTime ocr, LocalDateTime ai) {
		static StaleBefore from(LocalDateTime now, long ocrLockLeaseSeconds, long aiLockLeaseSeconds) {
			return new StaleBefore(
				now.minusSeconds(ocrLockLeaseSeconds),
				now.minusSeconds(aiLockLeaseSeconds)
			);
		}
	}

	private record Counts(
		long ocrDone,
		long aiDone,
		long failed,
		long ocrBacklog,
		long aiBacklog
	) {}
}
