package cmc.delta.domain.problem.application.validation.query;

import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanDetailProjection;
import org.springframework.stereotype.Component;

@Component
public class ProblemScanDetailValidator {

	public void validateOriginalAsset(ScanDetailProjection p) {
		if (p.getAssetId() == null || p.getStorageKey() == null || p.getStorageKey().isBlank()) {
			throw ProblemException.originalAssetNotFound();
		}
	}
}
