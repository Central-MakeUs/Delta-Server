package cmc.delta.domain.problem.application.support;

import cmc.delta.domain.problem.application.port.out.asset.AssetRepositoryPort;
import cmc.delta.domain.problem.model.asset.Asset;
import java.util.HashMap;
import java.util.Map;
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

	public int count() {
		return store.size();
	}

	public Asset get(Long id) {
		Asset a = store.get(id);
		if (a == null) throw new IllegalStateException("asset not found id=" + id);
		return a;
	}
}
