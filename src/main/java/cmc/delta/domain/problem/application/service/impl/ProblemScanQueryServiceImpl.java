package cmc.delta.domain.problem.application.service.impl;

import cmc.delta.domain.problem.api.dto.response.ProblemScanDetailResponse;
import cmc.delta.domain.problem.application.service.ProblemScanQueryService;
import cmc.delta.domain.problem.persistence.ProblemScanDetailProjection;
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
	private final StorageService storageService;

	@Transactional(readOnly = true)
	@Override
	public ProblemScanDetailResponse getDetail(Long userId, Long scanId) {
		ProblemScanDetailProjection p = scanRepository.findOwnedDetail(scanId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_SCAN_NOT_FOUND, "스캔을 찾을 수 없습니다."));

		if (p.getAssetId() == null || p.getStorageKey() == null) {
			throw new BusinessException(ErrorCode.PROBLEM_ASSET_NOT_FOUND, "원본 이미지를 찾을 수 없습니다.");
		}

		StoragePresignedGetData presigned = storageService.issueReadUrl(p.getStorageKey(), null);

		return new ProblemScanDetailResponse(
			p.getScanId(),
			p.getStatus().name(),
			Boolean.TRUE.equals(p.getHasFigure()),
			p.getRenderMode().name(),
			new ProblemScanDetailResponse.OriginalImage(
				p.getAssetId(),
				presigned.url(),
				p.getWidth(),
				p.getHeight()
			),
			p.getOcrPlainText(),
			p.getAiProblemLatex(),
			p.getAiSolutionLatex(),
			p.getCreatedAt(),
			p.getOcrCompletedAt(),
			p.getAiCompletedAt(),
			p.getFailReason()
		);
	}
}
