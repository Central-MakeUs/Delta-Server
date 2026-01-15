package cmc.delta.domain.problem.application.query;

import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanDetailResponse;

public interface ProblemScanQueryService {
	ProblemScanDetailResponse getDetail(Long userId, Long scanId);
}
