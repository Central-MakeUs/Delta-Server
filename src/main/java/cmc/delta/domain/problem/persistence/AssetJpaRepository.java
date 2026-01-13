package cmc.delta.domain.problem.persistence;

import cmc.delta.domain.problem.model.Asset;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetJpaRepository extends JpaRepository<Asset, Long> {

	@Query("""
		select a from Asset a
		where a.scan.id = :scanId
		  and a.assetType = cmc.delta.domain.problem.model.enums.AssetType.ORIGINAL
		  and a.slot = 0
		""")
	Optional<Asset> findOriginalByScanId(@Param("scanId") Long scanId);
}
