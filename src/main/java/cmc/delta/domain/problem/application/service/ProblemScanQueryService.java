package cmc.delta.domain.problem.application.service;

import cmc.delta.domain.problem.api.dto.response.ProblemScanDetailResponse;

public interface ProblemScanQueryService {
	ProblemScanDetailResponse getDetail(Long userId, Long scanId);
}
