package cmc.delta.domain.problem.adapter.in.monitor;

import java.util.UUID;

import cmc.delta.domain.problem.application.monitor.ProblemScanMetricsService;
import cmc.delta.domain.problem.application.monitor.WorkerMetricsSnapshot;
import cmc.delta.domain.problem.adapter.in.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.properties.OcrWorkerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProblemScanMetricsScheduler {

	private static final long WINDOW_MINUTES = 30L;
	private static final String MDC_TRACE_ID = "traceId";

	private final ProblemScanMetricsService metricsService;
	private final OcrWorkerProperties ocrProps;
	private final AiWorkerProperties aiProps;

	@Scheduled(fixedDelayString = "${worker.metrics.fixed-delay-ms:1800000}")
	public void logLast30Minutes() {
		String traceId = UUID.randomUUID().toString().replace("-", "");
		MDC.put(MDC_TRACE_ID, traceId);

		try {
			WorkerMetricsSnapshot s = metricsService.lastMinutes(
				WINDOW_MINUTES,
				ocrProps.lockLeaseSeconds(),
				aiProps.lockLeaseSeconds()
			);

			log.info(
				"event=worker.metrics windowMin={} from={} to={} ocrDone={} aiDone={} failed={} ocrBacklog={} aiBacklog={}",
				s.windowMinutes(), s.from(), s.to(),
				s.ocrDoneCount(), s.aiDoneCount(),
				s.failedCount(),
				s.ocrBacklog(), s.aiBacklog()
			);
		} finally {
			MDC.remove(MDC_TRACE_ID);
		}
	}
}
