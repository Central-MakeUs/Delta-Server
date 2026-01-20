package cmc.delta.domain.problem.persistence.problem.query;

import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemStatsCondition;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemTypeStatsRow;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemUnitStatsRow;
import java.util.List;

public interface ProblemStatsQueryRepository {

	List<ProblemUnitStatsRow> findUnitStats(Long userId, ProblemStatsCondition condition);

	List<ProblemTypeStatsRow> findTypeStats(Long userId, ProblemStatsCondition condition);
}
