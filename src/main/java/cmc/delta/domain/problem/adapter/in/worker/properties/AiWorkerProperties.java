package cmc.delta.domain.problem.adapter.in.worker.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker.ai")
public record AiWorkerProperties(
	long fixedDelayMs,
	int batchSize,
	long lockLeaseSeconds,
	int concurrency,
	int backlogLogMinutes) {
	public AiWorkerProperties {
		if (backlogLogMinutes <= 0) {
			backlogLogMinutes = 5;
		}
	}
}
