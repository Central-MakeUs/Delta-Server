package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemStatsResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemTypeStatsItemResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemUnitStatsItemResponse;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemStatsCondition;

public interface ProblemStatsQueryService {
	ProblemStatsResponse<ProblemUnitStatsItemResponse> getUnitStats(Long userId, ProblemStatsCondition condition);
	ProblemStatsResponse<ProblemTypeStatsItemResponse> getTypeStats(Long userId, ProblemStatsCondition condition);
}
