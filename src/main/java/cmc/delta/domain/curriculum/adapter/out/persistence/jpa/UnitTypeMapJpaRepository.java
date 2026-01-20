package cmc.delta.domain.curriculum.adapter.out.persistence.jpa;

import cmc.delta.domain.curriculum.model.UnitTypeMap;
import cmc.delta.domain.curriculum.model.UnitTypeMapId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitTypeMapJpaRepository extends JpaRepository<UnitTypeMap, UnitTypeMapId> {
	// TODO: 쿼리는 나중에 추가
}
