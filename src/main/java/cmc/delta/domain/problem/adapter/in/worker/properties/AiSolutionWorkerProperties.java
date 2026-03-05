package cmc.delta.domain.problem.adapter.in.worker.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker.ai-solution")
public record AiSolutionWorkerProperties(
	long fixedDelayMs,
	int batchSize) {

	private static final long DEFAULT_FIXED_DELAY_MS = 1000;
	private static final int DEFAULT_BATCH_SIZE = 3;

	public AiSolutionWorkerProperties {
		if (fixedDelayMs <= 0) {
			fixedDelayMs = DEFAULT_FIXED_DELAY_MS;
		}
		if (batchSize <= 0) {
			batchSize = DEFAULT_BATCH_SIZE;
		}
	}
}
