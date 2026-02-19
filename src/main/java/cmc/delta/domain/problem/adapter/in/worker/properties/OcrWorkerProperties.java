package cmc.delta.domain.problem.adapter.in.worker.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker.ocr")
public record OcrWorkerProperties(
	long fixedDelayMs,
	int batchSize,
	long lockLeaseSeconds,
	int concurrency,
	int backlogLogMinutes) {

	private static final int MIN_BACKLOG_LOG_MINUTES = 1;
	private static final int DEFAULT_BACKLOG_LOG_MINUTES = 5;

	public OcrWorkerProperties {
		backlogLogMinutes = normalizeBacklogLogMinutes(backlogLogMinutes);
	}

	private int normalizeBacklogLogMinutes(int value) {
		if (value < MIN_BACKLOG_LOG_MINUTES) {
			return DEFAULT_BACKLOG_LOG_MINUTES;
		}
		return value;
	}
}
