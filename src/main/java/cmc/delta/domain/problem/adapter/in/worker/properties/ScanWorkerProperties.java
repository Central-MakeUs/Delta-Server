package cmc.delta.domain.problem.adapter.in.worker.properties;

public record ScanWorkerProperties(
	long fixedDelayMs,
	int batchSize,
	long lockLeaseSeconds,
	int concurrency,
	int backlogLogMinutes) {
}
