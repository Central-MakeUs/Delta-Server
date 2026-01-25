package cmc.delta.domain.problem.adapter.in.monitor;

import cmc.delta.domain.problem.adapter.in.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.properties.OcrWorkerProperties;
import cmc.delta.domain.problem.application.monitor.ProblemScanMetricsService;
import cmc.delta.domain.problem.application.monitor.WorkerMetricsSnapshot;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProblemScanMetricsScheduler {

	private static final String MDC_TRACE_ID = "traceId";

	private final ProblemScanMetricsService metricsService;
	private final OcrWorkerProperties ocrProps;
	private final AiWorkerProperties aiProps;

	@Value("${worker.metrics.window-minutes:30}")
	private long windowMinutes;

	@Scheduled(fixedDelayString = "${worker.metrics.fixed-delay-ms:1800000}")
	public void logWindowMetrics() {
		withTraceId(() -> {
			WorkerMetricsSnapshot s = loadSnapshot();
			logSnapshot(s);
		});
	}

	private WorkerMetricsSnapshot loadSnapshot() {
		return metricsService.lastMinutes(
			windowMinutes,
			ocrProps.lockLeaseSeconds(),
			aiProps.lockLeaseSeconds());
	}

	private void logSnapshot(WorkerMetricsSnapshot s) {
		log.info(
			"event=worker.metrics windowMin={} from={} to={} ocrDone={} aiDone={} failed={} ocrBacklog={} aiBacklog={}",
			s.windowMinutes(), s.from(), s.to(),
			s.ocrDoneCount(), s.aiDoneCount(),
			s.failedCount(),
			s.ocrBacklog(), s.aiBacklog());
	}

	private void withTraceId(Runnable action) {
		String prev = MDC.get(MDC_TRACE_ID);
		MDC.put(MDC_TRACE_ID, newTraceId());

		try {
			action.run();
		} finally {
			restoreTraceId(prev);
		}
	}

	private String newTraceId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	private void restoreTraceId(String prev) {
		if (prev == null) {
			MDC.remove(MDC_TRACE_ID);
		} else {
			MDC.put(MDC_TRACE_ID, prev);
		}
	}
}
