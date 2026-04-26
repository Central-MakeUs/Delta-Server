package cmc.delta.domain.stats.adapter.out.persistence;

import cmc.delta.domain.stats.application.port.out.PeriodStatsCountResult;
import cmc.delta.domain.stats.application.port.out.StatsCountQueryPort;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StatsCountQueryAdapter implements StatsCountQueryPort {

	private final EntityManager em;

	@Override
	public PeriodStatsCountResult countAll(LocalDateTime from, LocalDateTime to) {
		Object[] row = (Object[]) em.createNativeQuery("""
			SELECT
			  (SELECT COUNT(*) FROM users                    WHERE created_at   BETWEEN :from AND :to),
			  (SELECT COUNT(*) FROM problem_scan             WHERE created_at   BETWEEN :from AND :to),
			  (SELECT COUNT(*) FROM problem                  WHERE created_at   BETWEEN :from AND :to),
			  (SELECT COUNT(*) FROM problem_ai_solution_task WHERE requested_at BETWEEN :from AND :to)
			""")
			.setParameter("from", from)
			.setParameter("to", to)
			.getSingleResult();

		return new PeriodStatsCountResult(
			toLong(row[0]),
			toLong(row[1]),
			toLong(row[2]),
			toLong(row[3])
		);
	}

	private long toLong(Object value) {
		return ((Number) value).longValue();
	}
}
