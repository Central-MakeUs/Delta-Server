package cmc.delta.domain.curriculum.adapter.out.persistence.adapter;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.UnitJpaRepository;
import cmc.delta.domain.curriculum.application.port.out.UnitRepositoryPort;
import cmc.delta.domain.curriculum.model.Unit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UnitRepositoryAdapter implements UnitRepositoryPort {

	private final UnitJpaRepository unitJpaRepository;

	@Override
	public List<Unit> findAllRootUnitsActive() {
		return unitJpaRepository.findAllRootUnitsActive();
	}

	@Override
	public List<Unit> findAllChildUnitsActive() {
		return unitJpaRepository.findAllChildUnitsActive();
	}
}
