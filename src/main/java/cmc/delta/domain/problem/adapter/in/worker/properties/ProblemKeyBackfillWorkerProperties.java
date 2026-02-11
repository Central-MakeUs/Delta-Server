package cmc.delta.domain.problem.adapter.in.worker.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker.problem-key-backfill")
public record ProblemKeyBackfillWorkerProperties(
	long fixedDelayMs,
	int batchSize) {

	private static final long MIN_FIXED_DELAY_MS = 1L;
	private static final long DEFAULT_FIXED_DELAY_MS = 600_000L;

	private static final int MIN_BATCH_SIZE = 1;
	private static final int DEFAULT_BATCH_SIZE = 200;

	public ProblemKeyBackfillWorkerProperties {
		fixedDelayMs = normalizeFixedDelayMs(fixedDelayMs);
		batchSize = normalizeBatchSize(batchSize);
	}

	private long normalizeFixedDelayMs(long value) {
		if (value < MIN_FIXED_DELAY_MS) {
			return DEFAULT_FIXED_DELAY_MS;
		}
		return value;
	}

	private int normalizeBatchSize(int value) {
		if (value < MIN_BATCH_SIZE) {
			return DEFAULT_BATCH_SIZE;
		}
		return value;
	}
}
