package cmc.delta.domain.problem.application.port.out.problem.query;

import cmc.delta.domain.problem.application.port.in.problem.query.ProblemStatsCondition;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemMonthlyProgressRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeStatsRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemUnitStatsRow;
import java.time.LocalDateTime;
import java.util.List;

public interface ProblemStatsQueryPort {

	List<ProblemUnitStatsRow> findUnitStats(Long userId, ProblemStatsCondition condition);

	List<ProblemTypeStatsRow> findTypeStats(Long userId, ProblemStatsCondition condition);

	ProblemMonthlyProgressRow findMonthlyProgress(Long userId, LocalDateTime fromInclusive, LocalDateTime toExclusive);
}
