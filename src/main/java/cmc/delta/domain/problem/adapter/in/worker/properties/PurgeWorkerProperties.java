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

	private static final int MIN_RETENTION_DAYS = 1;
	private static final int DEFAULT_RETENTION_DAYS = 3;

	public PurgeWorkerProperties {
		fixedDelayMs = WorkerPropertiesNormalizer.normalizeFixedDelayMs(fixedDelayMs);
		batchSize = WorkerPropertiesNormalizer.normalizeBatchSize(batchSize);
		lockLeaseSeconds = WorkerPropertiesNormalizer.normalizeLockLeaseSeconds(lockLeaseSeconds);
		concurrency = WorkerPropertiesNormalizer.normalizeConcurrency(concurrency);
		backlogLogMinutes = WorkerPropertiesNormalizer.normalizeBacklogLogMinutes(backlogLogMinutes);
		retentionDays = retentionDays < MIN_RETENTION_DAYS ? DEFAULT_RETENTION_DAYS : retentionDays;
	}
}
