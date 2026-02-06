package cmc.delta.domain.problem.application.port.out.asset;

import cmc.delta.domain.problem.model.asset.Asset;
import java.util.Optional;

public interface AssetRepositoryPort {
	Asset save(Asset asset);

	Optional<Asset> findOriginalByScanId(Long scanId);
}
