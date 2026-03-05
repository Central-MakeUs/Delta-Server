package cmc.delta.domain.problem.adapter.in.worker.scheduler;

import cmc.delta.domain.problem.adapter.in.worker.ProblemAiSolutionWorker;
import cmc.delta.domain.problem.adapter.in.worker.properties.AiSolutionWorkerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemAiSolutionWorkerScheduler {

	private final ProblemAiSolutionWorker worker;
	private final AiSolutionWorkerProperties properties;

	@Scheduled(fixedDelayString = "${worker.ai-solution.fixed-delay-ms:1000}")
	public void tick() {
		worker.runBatch(properties.batchSize());
	}
}
