package cmc.delta.domain.problem.application.port.out.problem.query;

import java.util.List;

import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemStatsCondition;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemTypeStatsRow;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemUnitStatsRow;

public interface ProblemStatsQueryPort {

	List<ProblemUnitStatsRow> findUnitStats(Long userId, ProblemStatsCondition condition);

	List<ProblemTypeStatsRow> findTypeStats(Long userId, ProblemStatsCondition condition);
}
