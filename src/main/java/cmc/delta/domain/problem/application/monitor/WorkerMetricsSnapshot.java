package cmc.delta.domain.problem.application.monitor;

import java.time.LocalDateTime;

public record WorkerMetricsSnapshot(
	long windowMinutes,
	LocalDateTime from,
	LocalDateTime to,
	long ocrDoneCount,
	long aiDoneCount,
	long failedCount,
	long ocrBacklog,
	long aiBacklog
) {}
