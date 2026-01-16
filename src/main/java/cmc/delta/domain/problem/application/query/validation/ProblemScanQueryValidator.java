package cmc.delta.domain.problem.application.query.validation;

import cmc.delta.domain.problem.application.common.exception.ProblemAssetNotFoundException;
import cmc.delta.domain.problem.persistence.scan.dto.ProblemScanSummaryRow;
import org.springframework.stereotype.Component;

@Component
public class ProblemScanQueryValidator {

	public void validateHasOriginalAsset(ProblemScanSummaryRow row) {
		if (row.getAssetId() == null || row.getStorageKey() == null) {
			throw new ProblemAssetNotFoundException();
		}
	}
}
