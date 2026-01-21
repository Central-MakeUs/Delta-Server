package cmc.delta.domain.problem.application.port.out.asset;

import cmc.delta.domain.problem.model.asset.Asset;

public interface AssetRepositoryPort {
	Asset save(Asset asset);
}
