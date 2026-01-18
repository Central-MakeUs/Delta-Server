package cmc.delta.domain.problem.application.query.validation;

import cmc.delta.domain.problem.application.common.exception.ProblemAssetNotFoundException;
import cmc.delta.domain.problem.persistence.scan.query.dto.ProblemScanListRow;
import org.springframework.stereotype.Component;

@Component
public class ProblemScanQueryValidator {

	public void validateHasOriginalAsset(ProblemScanListRow row) {
		if (row.getAssetId() == null || row.getStorageKey() == null) {
			throw new ProblemAssetNotFoundException();
		}
	}
}
