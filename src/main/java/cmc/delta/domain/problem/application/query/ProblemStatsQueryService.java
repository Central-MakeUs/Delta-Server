package cmc.delta.domain.problem.application.query;

import cmc.delta.domain.problem.api.problem.dto.response.ProblemStatsResponse;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemTypeStatsItemResponse;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemUnitStatsItemResponse;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemStatsCondition;

public interface ProblemStatsQueryService {
	ProblemStatsResponse<ProblemUnitStatsItemResponse> getUnitStats(Long userId, ProblemStatsCondition condition);
	ProblemStatsResponse<ProblemTypeStatsItemResponse> getTypeStats(Long userId, ProblemStatsCondition condition);
}
