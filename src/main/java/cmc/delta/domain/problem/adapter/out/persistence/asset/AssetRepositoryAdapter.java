package cmc.delta.domain.problem.adapter.out.persistence.asset;

import cmc.delta.domain.problem.application.port.out.asset.AssetRepositoryPort;
import cmc.delta.domain.problem.model.asset.Asset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssetRepositoryAdapter implements AssetRepositoryPort {

	private final AssetJpaRepository assetJpaRepository;

	@Override
	public Asset save(Asset asset) {
		return assetJpaRepository.save(asset);
	}
}
