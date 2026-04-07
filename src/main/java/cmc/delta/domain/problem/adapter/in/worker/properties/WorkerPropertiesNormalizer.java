package cmc.delta.domain.problem.adapter.in.worker.properties;

final class WorkerPropertiesNormalizer {

	private static final long MIN_FIXED_DELAY_MS = 1L;
	private static final long DEFAULT_FIXED_DELAY_MS = 500L;

	private static final int MIN_BATCH_SIZE = 1;
	private static final int DEFAULT_BATCH_SIZE = 10;

	private static final long MIN_LOCK_LEASE_SECONDS = 1L;
	private static final long DEFAULT_LOCK_LEASE_SECONDS = 60L;

	private static final int MIN_CONCURRENCY = 1;
	private static final int DEFAULT_CONCURRENCY = 1;

	private static final int MIN_BACKLOG_LOG_MINUTES = 1;
	private static final int DEFAULT_BACKLOG_LOG_MINUTES = 5;

	private WorkerPropertiesNormalizer() {
	}

	static long normalizeFixedDelayMs(long value) {
		return value < MIN_FIXED_DELAY_MS ? DEFAULT_FIXED_DELAY_MS : value;
	}

	static int normalizeBatchSize(int value) {
		return value < MIN_BATCH_SIZE ? DEFAULT_BATCH_SIZE : value;
	}

	static long normalizeLockLeaseSeconds(long value) {
		return value < MIN_LOCK_LEASE_SECONDS ? DEFAULT_LOCK_LEASE_SECONDS : value;
	}

	static int normalizeConcurrency(int value) {
		return value < MIN_CONCURRENCY ? DEFAULT_CONCURRENCY : value;
	}

	static int normalizeBacklogLogMinutes(int value) {
		return value < MIN_BACKLOG_LOG_MINUTES ? DEFAULT_BACKLOG_LOG_MINUTES : value;
	}
}
