package cmc.delta.domain.problem.application.validation.query;

import cmc.delta.domain.problem.application.exception.ProblemAssetNotFoundException;
import cmc.delta.domain.problem.adapter.out.persistence.scan.query.dto.ScanListRow;
import org.springframework.stereotype.Component;

@Component
public class ProblemScanQueryValidator {

	public void validateHasOriginalAsset(ScanListRow row) {
		if (row.getAssetId() == null || row.getStorageKey() == null) {
			throw new ProblemAssetNotFoundException();
		}
	}
}
