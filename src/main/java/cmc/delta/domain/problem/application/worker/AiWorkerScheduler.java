package cmc.delta.domain.problem.application.worker;

import cmc.delta.domain.problem.application.worker.support.AiWorkerProperties;
import cmc.delta.domain.problem.application.worker.support.LockOwnerProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiWorkerScheduler {

	private final AiScanWorker aiScanWorker;
	private final LockOwnerProvider lockOwnerProvider;
	private final AiWorkerProperties props;

	@Scheduled(fixedDelayString = "${worker.ai.fixed-delay-ms:2000}")
	public void tick() {
		aiScanWorker.runBatch(
			lockOwnerProvider.get(),
			props.batchSize(),
			props.lockLeaseSeconds()
		);
	}
}
