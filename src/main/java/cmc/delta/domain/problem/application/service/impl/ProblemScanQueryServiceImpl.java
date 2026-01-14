package cmc.delta.domain.problem.application.service.impl;

import cmc.delta.domain.problem.api.dto.response.ProblemScanDetailResponse;
import cmc.delta.domain.problem.application.service.ProblemScanQueryService;
import cmc.delta.domain.problem.model.Asset;
import cmc.delta.domain.problem.model.ProblemScan;
import cmc.delta.domain.problem.persistence.AssetJpaRepository;
import cmc.delta.domain.problem.persistence.ProblemScanJpaRepository;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import cmc.delta.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemScanQueryServiceImpl implements ProblemScanQueryService {

	private final ProblemScanJpaRepository scanRepository;
	private final AssetJpaRepository assetRepository;
	private final StorageService storageService;

	@Transactional(readOnly = true)
	@Override
	public ProblemScanDetailResponse getDetail(Long userId, Long scanId) {
		ProblemScan scan = scanRepository.findOwnedBy(scanId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_SCAN_NOT_FOUND, "스캔을 찾을 수 없습니다."));

		Asset original = assetRepository.findOriginalByScanId(scanId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_ASSET_NOT_FOUND, "원본 이미지를 찾을 수 없습니다."));

		StoragePresignedGetData presigned = storageService.issueReadUrl(original.getStorageKey(), null);

		return new ProblemScanDetailResponse(
			scan.getId(),
			scan.getStatus().name(),
			scan.isHasFigure(),
			scan.getRenderMode().name(),
			new ProblemScanDetailResponse.OriginalImage(
				original.getId(),
				presigned.url(),
				original.getWidth(),
				original.getHeight()
			),
			scan.getOcrPlainText(),
			scan.getAiProblemLatex(),
			scan.getAiSolutionLatex(),
			scan.getCreatedAt(),
			scan.getOcrCompletedAt(),
			scan.getAiCompletedAt(),
			scan.getFailReason()
		);
	}
}
