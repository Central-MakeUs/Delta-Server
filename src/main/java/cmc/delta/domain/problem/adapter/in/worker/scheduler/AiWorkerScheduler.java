package cmc.delta.domain.problem.adapter.in.worker.scheduler;

import cmc.delta.domain.problem.adapter.in.worker.AiScanWorker;
import cmc.delta.domain.problem.adapter.in.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.support.LockOwnerProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiWorkerScheduler {

	private final AiScanWorker aiScanWorker;
	private final LockOwnerProvider lockOwnerProvider;
	private final AiWorkerProperties props;

	@Scheduled(fixedDelayString = "${worker.ai.fixed-delay-ms:2000}")
	public void tick() {
		runBatch();
	}

	private void runBatch() {
		aiScanWorker.runBatch(
			lockOwnerProvider.get(),
			props.batchSize(),
			props.lockLeaseSeconds());
	}
}
