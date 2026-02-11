package cmc.delta.domain.user.adapter.in.worker.scheduler;

import cmc.delta.domain.user.adapter.in.worker.UserPurgeWorker;
import cmc.delta.domain.user.adapter.in.worker.properties.UserPurgeWorkerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPurgeWorkerScheduler {

	private final UserPurgeWorker worker;
	private final UserPurgeWorkerProperties props;

	@Scheduled(fixedDelayString = "${worker.user-purge.fixed-delay-ms:3600000}")
	public void tick() {
		worker.runBatch(props.batchSize(), props.retentionDays());
	}
}
