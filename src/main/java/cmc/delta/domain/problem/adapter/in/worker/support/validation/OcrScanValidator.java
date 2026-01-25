package cmc.delta.domain.problem.adapter.in.worker.support.validation;

import cmc.delta.domain.problem.adapter.in.worker.exception.OriginalAssetNotFoundException;
import cmc.delta.domain.problem.adapter.out.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.model.asset.Asset;
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
