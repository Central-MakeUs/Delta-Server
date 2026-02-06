package cmc.delta.domain.problem.application.support;

import cmc.delta.domain.problem.application.port.out.asset.AssetRepositoryPort;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.enums.AssetType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryAssetRepositoryPort implements AssetRepositoryPort {

	private final Map<Long, Asset> store = new HashMap<>();
	private final AtomicLong seq = new AtomicLong(0);

	@Override
	public Asset save(Asset asset) {
		if (asset.getId() == null) {
			ReflectionIds.setId(asset, seq.incrementAndGet());
		}
		store.put(asset.getId(), asset);
		return asset;
	}

	@Override
	public Optional<Asset> findOriginalByScanId(Long scanId) {
		if (scanId == null) {
			return Optional.empty();
		}
		return store.values().stream()
			.filter(a -> a.getScan() != null && scanId.equals(a.getScan().getId()))
			.filter(a -> a.getAssetType() == AssetType.ORIGINAL)
			.filter(a -> a.getSlot() == 0)
			.findFirst();
	}

	public int count() {
		return store.size();
	}

	public Asset get(Long id) {
		Asset a = store.get(id);
		if (a == null)
			throw new IllegalStateException("asset not found id=" + id);
		return a;
	}
}
