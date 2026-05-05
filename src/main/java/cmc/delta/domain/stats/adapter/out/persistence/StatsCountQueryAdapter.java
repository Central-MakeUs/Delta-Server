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
			  (SELECT COUNT(*) FROM users u
			   WHERE u.created_at BETWEEN :from AND :to AND u.role != 'ADMIN'),
			  (SELECT COUNT(*) FROM problem_scan ps
			   JOIN users u ON u.id = ps.user_id
			   WHERE ps.created_at BETWEEN :from AND :to AND u.role != 'ADMIN'),
			  (SELECT COUNT(*) FROM problem p
			   JOIN users u ON u.id = p.user_id
			   WHERE p.created_at BETWEEN :from AND :to AND u.role != 'ADMIN'),
			  (SELECT COUNT(*) FROM problem_ai_solution_task t
			   JOIN problem p ON p.id = t.problem_id
			   JOIN users u ON u.id = p.user_id
			   WHERE t.requested_at BETWEEN :from AND :to AND u.role != 'ADMIN')
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
