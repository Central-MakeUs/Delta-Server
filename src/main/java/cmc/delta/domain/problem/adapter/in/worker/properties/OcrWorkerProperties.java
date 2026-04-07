package cmc.delta.domain.problem.adapter.in.worker.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker.ocr")
public record OcrWorkerProperties(
	long fixedDelayMs,
	int batchSize,
	long lockLeaseSeconds,
	int concurrency,
	int backlogLogMinutes) {

	public OcrWorkerProperties {
		fixedDelayMs = WorkerPropertiesNormalizer.normalizeFixedDelayMs(fixedDelayMs);
		batchSize = WorkerPropertiesNormalizer.normalizeBatchSize(batchSize);
		lockLeaseSeconds = WorkerPropertiesNormalizer.normalizeLockLeaseSeconds(lockLeaseSeconds);
		concurrency = WorkerPropertiesNormalizer.normalizeConcurrency(concurrency);
		backlogLogMinutes = WorkerPropertiesNormalizer.normalizeBacklogLogMinutes(backlogLogMinutes);
	}

}
