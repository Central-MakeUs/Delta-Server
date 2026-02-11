package cmc.delta.domain.problem.adapter.in.worker.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker.purge")
public record PurgeWorkerProperties(
	long fixedDelayMs,
	int batchSize,
	long lockLeaseSeconds,
	int concurrency,
	int backlogLogMinutes,
	int retentionDays) {

	private static final long MIN_FIXED_DELAY_MS = 1L;
	private static final long DEFAULT_FIXED_DELAY_MS = 3_600_000L;

	private static final int MIN_BATCH_SIZE = 1;
	private static final int DEFAULT_BATCH_SIZE = 50;

	private static final long MIN_LOCK_LEASE_SECONDS = 1L;
	private static final long DEFAULT_LOCK_LEASE_SECONDS = 60L;

	private static final int MIN_CONCURRENCY = 1;
	private static final int DEFAULT_CONCURRENCY = 1;

	private static final int MIN_BACKLOG_LOG_MINUTES = 1;
	private static final int DEFAULT_BACKLOG_LOG_MINUTES = 60;

	private static final int MIN_RETENTION_DAYS = 1;
	private static final int DEFAULT_RETENTION_DAYS = 3;

	public PurgeWorkerProperties {
		fixedDelayMs = normalizeFixedDelayMs(fixedDelayMs);
		batchSize = normalizeBatchSize(batchSize);
		lockLeaseSeconds = normalizeLockLeaseSeconds(lockLeaseSeconds);
		concurrency = normalizeConcurrency(concurrency);
		backlogLogMinutes = normalizeBacklogLogMinutes(backlogLogMinutes);
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

	private long normalizeLockLeaseSeconds(long value) {
		if (value < MIN_LOCK_LEASE_SECONDS) {
			return DEFAULT_LOCK_LEASE_SECONDS;
		}
		return value;
	}

	private int normalizeConcurrency(int value) {
		if (value < MIN_CONCURRENCY) {
			return DEFAULT_CONCURRENCY;
		}
		return value;
	}

	private int normalizeBacklogLogMinutes(int value) {
		if (value < MIN_BACKLOG_LOG_MINUTES) {
			return DEFAULT_BACKLOG_LOG_MINUTES;
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
