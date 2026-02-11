package cmc.delta.domain.problem.adapter.in.worker.scheduler;

import cmc.delta.domain.problem.adapter.in.worker.ScanPurgeWorker;
import cmc.delta.domain.problem.adapter.in.worker.properties.PurgeWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.support.LockOwnerProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurgeWorkerScheduler {

	private final ScanPurgeWorker scanPurgeWorker;
	private final LockOwnerProvider lockOwnerProvider;
	private final PurgeWorkerProperties props;

	@Scheduled(fixedDelayString = "${worker.purge.fixed-delay-ms:3600000}")
	public void tick() {
		runBatch();
	}

	private void runBatch() {
		scanPurgeWorker.runBatch(
			lockOwnerProvider.get(),
			props.batchSize(),
			props.lockLeaseSeconds(),
			props.retentionDays());
	}
}
