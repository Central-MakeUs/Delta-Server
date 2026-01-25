package cmc.delta.domain.problem.application.validation.query;

import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanListRow;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.global.error.ErrorCode;

import org.springframework.stereotype.Component;

@Component
public class ProblemScanQueryValidator {

	public void validateHasOriginalAsset(ScanListRow row) {
		if (row.getAssetId() == null || row.getStorageKey() == null) {
			throw new ProblemException(ErrorCode.PROBLEM_ASSET_NOT_FOUND);
		}
	}
}
