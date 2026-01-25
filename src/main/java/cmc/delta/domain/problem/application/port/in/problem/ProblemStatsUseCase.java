package cmc.delta.domain.problem.application.port.in.problem;

import cmc.delta.domain.problem.application.port.in.problem.query.ProblemStatsCondition;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemStatsResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemTypeStatsItemResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemUnitStatsItemResponse;

public interface ProblemStatsUseCase {

	ProblemStatsResponse<ProblemUnitStatsItemResponse> getUnitStats(Long userId, ProblemStatsCondition condition);

	ProblemStatsResponse<ProblemTypeStatsItemResponse> getTypeStats(Long userId, ProblemStatsCondition condition);
}
