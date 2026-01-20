package cmc.delta.domain.problem.adapter.in.worker.support.logging;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.LongConsumer;
import org.springframework.stereotype.Component;

@Component
public class BacklogLogger {

	private final ConcurrentHashMap<String, LocalDateTime> lastLoggedAtByKey;

	public BacklogLogger() {
		this.lastLoggedAtByKey = new ConcurrentHashMap<>();
	}

	public void logIfDue(
		String key,
		LocalDateTime now,
		long intervalMinutes,
		LongSupplier backlogSupplier,
		LongConsumer logAction
	) {
		LocalDateTime lastLoggedAt = lastLoggedAtByKey.get(key);
		Duration interval = Duration.ofMinutes(Math.max(1, intervalMinutes));

		if (lastLoggedAt != null && now.isBefore(lastLoggedAt.plus(interval))) {
			return;
		}

		long backlog = backlogSupplier.getAsLong();
		logAction.accept(backlog);
		lastLoggedAtByKey.put(key, now);
	}
}
