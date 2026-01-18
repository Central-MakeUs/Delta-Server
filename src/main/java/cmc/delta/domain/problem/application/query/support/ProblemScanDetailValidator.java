package cmc.delta.domain.problem.application.query.support;

import cmc.delta.domain.problem.persistence.scan.query.projection.ScanDetailProjection;
import cmc.delta.domain.problem.persistence.scan.ScanRepository;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class ProblemScanDetailValidator {

	private static final String MSG_SCAN_NOT_FOUND = "스캔을 찾을 수 없습니다.";
	private static final String MSG_ASSET_NOT_FOUND = "원본 이미지를 찾을 수 없습니다.";

	public ScanDetailProjection getOwnedDetail(
		ScanRepository scanRepository,
		Long scanId,
		Long userId
	) {
		return scanRepository.findOwnedDetail(scanId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_SCAN_NOT_FOUND, MSG_SCAN_NOT_FOUND));
	}

	public void validateOriginalAsset(ScanDetailProjection p) {
		if (p.getAssetId() == null || p.getStorageKey() == null || p.getStorageKey().isBlank()) {
			throw new BusinessException(ErrorCode.PROBLEM_ASSET_NOT_FOUND, MSG_ASSET_NOT_FOUND);
		}
	}
}
