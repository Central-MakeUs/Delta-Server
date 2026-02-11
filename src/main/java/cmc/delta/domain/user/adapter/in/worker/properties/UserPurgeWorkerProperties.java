package cmc.delta.domain.user.adapter.in.worker.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker.user-purge")
public record UserPurgeWorkerProperties(
	long fixedDelayMs,
	int batchSize,
	int retentionDays) {

	private static final long MIN_FIXED_DELAY_MS = 1L;
	private static final long DEFAULT_FIXED_DELAY_MS = 3_600_000L;

	private static final int MIN_BATCH_SIZE = 1;
	private static final int DEFAULT_BATCH_SIZE = 50;

	private static final int MIN_RETENTION_DAYS = 1;
	private static final int DEFAULT_RETENTION_DAYS = 7;

	public UserPurgeWorkerProperties {
		fixedDelayMs = normalizeFixedDelayMs(fixedDelayMs);
		batchSize = normalizeBatchSize(batchSize);
		retentionDays = normalizeRetentionDays(retentionDays);
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

	private int normalizeRetentionDays(int value) {
		if (value < MIN_RETENTION_DAYS) {
			return DEFAULT_RETENTION_DAYS;
		}
		return value;
	}
}
