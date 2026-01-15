package cmc.delta.domain.problem.application.worker.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker.ocr")
public record OcrWorkerProperties(
	long fixedDelayMs,
	int batchSize,
	long lockLeaseSeconds,
	int concurrency,
	int backlogLogMinutes
) {
	public OcrWorkerProperties {
		if (backlogLogMinutes <= 0) {
			backlogLogMinutes = 5;
		}
	}
}
