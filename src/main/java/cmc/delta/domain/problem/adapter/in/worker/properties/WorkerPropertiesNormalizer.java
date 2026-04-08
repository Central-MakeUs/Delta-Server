package cmc.delta.domain.problem.adapter.in.worker.properties;

final class WorkerPropertiesNormalizer {

	private static final int MIN_BACKLOG_LOG_MINUTES = 1;
	private static final int DEFAULT_BACKLOG_LOG_MINUTES = 5;

	private WorkerPropertiesNormalizer() {
	}

	static int normalizeBacklogLogMinutes(int value) {
		return value < MIN_BACKLOG_LOG_MINUTES ? DEFAULT_BACKLOG_LOG_MINUTES : value;
	}
}
