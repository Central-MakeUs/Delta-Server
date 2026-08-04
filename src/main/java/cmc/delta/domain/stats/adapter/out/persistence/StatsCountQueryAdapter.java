package cmc.delta.domain.stats.adapter.out.persistence;

import cmc.delta.domain.stats.application.port.out.PeriodStatsCountResult;
import cmc.delta.domain.stats.application.port.out.StatsCountQueryPort;
import cmc.delta.domain.user.model.enums.UserRole;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StatsCountQueryAdapter implements StatsCountQueryPort {

	private static final String COUNT_ALL_SQL = """
		SELECT
		  (SELECT COUNT(*) FROM users u
		   WHERE u.created_at BETWEEN :from AND :to AND u.role != :adminRole),
		  (SELECT COUNT(*) FROM problem_scan ps
		   JOIN users u ON u.id = ps.user_id
		   WHERE ps.created_at BETWEEN :from AND :to AND u.role != :adminRole),
		  (SELECT COUNT(*) FROM problem p
		   JOIN users u ON u.id = p.user_id
		   WHERE p.created_at BETWEEN :from AND :to AND u.role != :adminRole),
		  (SELECT COUNT(*) FROM problem_ai_solution_task t
		   JOIN problem p ON p.id = t.problem_id
		   JOIN users u ON u.id = p.user_id
		   WHERE t.requested_at BETWEEN :from AND :to AND u.role != :adminRole)
		""";

	private static final int NEW_USERS_INDEX = 0;
	private static final int SCANS_INDEX = 1;
	private static final int PROBLEMS_INDEX = 2;
	private static final int AI_SOLUTION_ATTEMPTS_INDEX = 3;

	private final EntityManager em;

	@Override
	public PeriodStatsCountResult countAll(LocalDateTime from, LocalDateTime to) {
		Object[] row = (Object[])em.createNativeQuery(COUNT_ALL_SQL)
			.setParameter("from", from)
			.setParameter("to", to)
			.setParameter("adminRole", UserRole.ADMIN.name())
			.getSingleResult();

		return mapToResult(row);
	}

	private PeriodStatsCountResult mapToResult(Object[] row) {
		return new PeriodStatsCountResult(
			toLong(row[NEW_USERS_INDEX]),
			toLong(row[SCANS_INDEX]),
			toLong(row[PROBLEMS_INDEX]),
			toLong(row[AI_SOLUTION_ATTEMPTS_INDEX]));
	}

	private long toLong(Object value) {
		return ((Number)value).longValue();
	}
}
