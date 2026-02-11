package cmc.delta.domain.problem.adapter.in.worker.scheduler;

import cmc.delta.domain.problem.adapter.in.worker.ProblemKeyBackfillWorker;
import cmc.delta.domain.problem.adapter.in.worker.properties.ProblemKeyBackfillWorkerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProblemKeyBackfillScheduler {

	private final ProblemKeyBackfillWorker worker;
	private final ProblemKeyBackfillWorkerProperties props;

	@Scheduled(fixedDelayString = "${worker.problem-key-backfill.fixed-delay-ms:600000}")
	public void tick() {
		int processed = worker.runOnce(props.batchSize());
		if (processed > 0) {
			log.info("problem key backfill tick processed={}", processed);
		}
	}
}
