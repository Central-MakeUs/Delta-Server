package cmc.delta.domain.report.adapter.in.worker.scheduler;

import cmc.delta.domain.report.adapter.in.worker.ReportWorker;
import cmc.delta.domain.report.adapter.in.worker.properties.ReportWorkerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportWorkerScheduler {

	private final ReportWorker worker;
	private final ReportWorkerProperties properties;

	@Scheduled(fixedDelayString = "${worker.report.fixed-delay-ms:1000}")
	public void tick() {
		worker.runBatch(properties.batchSize());
	}
}
