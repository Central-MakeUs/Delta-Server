package cmc.delta.domain.problem.adapter.in.worker.support.logging;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BacklogLoggerTest {

	@Test
	@DisplayName("backlog log: 첫 호출은 로그하고, interval 내 재호출은 skip")
	void logIfDue_throttlesByInterval() {
		// given
		BacklogLogger logger = new BacklogLogger();
		AtomicLong logs = new AtomicLong(0);
		AtomicLong lastBacklog = new AtomicLong(-1);

		// when
		logger.logIfDue(
			"key",
			LocalDateTime.of(2026, 1, 1, 0, 0, 0),
			5,
			() -> 3,
			b -> {
				logs.incrementAndGet();
				lastBacklog.set(b);
			}
		);

		logger.logIfDue(
			"key",
			LocalDateTime.of(2026, 1, 1, 0, 4, 0),
			5,
			() -> 10,
			b -> {
				logs.incrementAndGet();
				lastBacklog.set(b);
			}
		);

		// then
		assertThat(logs.get()).isEqualTo(1);
		assertThat(lastBacklog.get()).isEqualTo(3);
	}

	@Test
	@DisplayName("backlog log: intervalMinutes가 0 이하면 최소 1분으로 보정")
	void logIfDue_intervalMinutesMin1() {
		// given
		BacklogLogger logger = new BacklogLogger();
		AtomicLong logs = new AtomicLong(0);

		// when
		logger.logIfDue(
			"key",
			LocalDateTime.of(2026, 1, 1, 0, 0, 0),
			0,
			() -> 1,
			b -> logs.incrementAndGet()
		);

		logger.logIfDue(
			"key",
			LocalDateTime.of(2026, 1, 1, 0, 0, 30),
			0,
			() -> 1,
			b -> logs.incrementAndGet()
		);

		logger.logIfDue(
			"key",
			LocalDateTime.of(2026, 1, 1, 0, 1, 0),
			0,
			() -> 1,
			b -> logs.incrementAndGet()
		);

		// then
		assertThat(logs.get()).isEqualTo(2);
	}
}
