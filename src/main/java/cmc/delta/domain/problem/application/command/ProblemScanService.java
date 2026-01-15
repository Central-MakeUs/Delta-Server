package cmc.delta.domain.problem.application.command;

import org.springframework.web.multipart.MultipartFile;

import cmc.delta.domain.problem.api.scan.dto.response.ProblemScanCreateResponse;

public interface ProblemScanService {
	ProblemScanCreateResponse createScan(Long userId, MultipartFile file);
	void retryFailed(Long userId, Long scanId);
}
