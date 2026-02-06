package cmc.delta.domain.problem.adapter.out.persistence.asset;

import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.enums.AssetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetJpaRepository extends JpaRepository<Asset, Long> {

	Optional<Asset> findFirstByScan_IdAndAssetTypeAndSlot(Long scanId, AssetType assetType, int slot);

	List<Asset> findAllByScan_Id(Long scanId);

	void deleteAllByScan_Id(Long scanId);

	default Optional<Asset> findOriginalByScanId(Long scanId) {
		return findFirstByScan_IdAndAssetTypeAndSlot(scanId, AssetType.ORIGINAL, 0);
	}
}
