package cmc.delta.domain.problem.application.worker.support.validation;

import cmc.delta.domain.problem.application.worker.exception.OriginalAssetNotFoundException;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.persistence.asset.AssetJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OcrScanValidator {

	private final AssetJpaRepository assetRepository;

	public OcrScanValidator(AssetJpaRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public Asset requireOriginalAsset(Long scanId) {
		Optional<Asset> optionalAsset = assetRepository.findOriginalByScanId(scanId);
		if (optionalAsset.isEmpty()) {
			throw new OriginalAssetNotFoundException(scanId);
		}
		return optionalAsset.get();
	}
}
