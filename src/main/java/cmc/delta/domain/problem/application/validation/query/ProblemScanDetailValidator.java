package cmc.delta.domain.problem.application.validation.query;

import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanDetailProjection;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class ProblemScanDetailValidator {

	private static final String MSG_ASSET_NOT_FOUND = "원본 이미지를 찾을 수 없습니다.";

	public void validateOriginalAsset(ScanDetailProjection p) {
		if (p.getAssetId() == null || p.getStorageKey() == null || p.getStorageKey().isBlank()) {
			throw new BusinessException(ErrorCode.PROBLEM_ASSET_NOT_FOUND, MSG_ASSET_NOT_FOUND);
		}
	}
}
