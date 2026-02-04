package cmc.delta.domain.problem.adapter.in.worker.support.logging;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import org.springframework.stereotype.Component;

@Component
public class BacklogLogger {

	private static final long MIN_INTERVAL_MINUTES = 1L;

	private final ConcurrentHashMap<String, LocalDateTime> lastLoggedAtByKey;

	public BacklogLogger() {
		this.lastLoggedAtByKey = new ConcurrentHashMap<>();
	}

	public void logIfDue(
		String key,
		LocalDateTime now,
		long intervalMinutes,
		LongSupplier backlogSupplier,
		LongConsumer logAction) {
		LocalDateTime lastLoggedAt = lastLoggedAtByKey.get(key);
		Duration interval = resolveInterval(intervalMinutes);

		if (lastLoggedAt != null && now.isBefore(lastLoggedAt.plus(interval))) {
			return;
		}

		long backlog = backlogSupplier.getAsLong();
		logAction.accept(backlog);
		lastLoggedAtByKey.put(key, now);
	}

	private Duration resolveInterval(long intervalMinutes) {
		long minutes = Math.max(MIN_INTERVAL_MINUTES, intervalMinutes);
		return Duration.ofMinutes(minutes);
	}
}
