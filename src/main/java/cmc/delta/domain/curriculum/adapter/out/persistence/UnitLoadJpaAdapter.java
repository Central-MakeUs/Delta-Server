package cmc.delta.domain.curriculum.adapter.out.persistence;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.UnitJpaRepository;
import cmc.delta.domain.curriculum.application.port.out.UnitLoadPort;
import cmc.delta.domain.curriculum.model.Unit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnitLoadJpaAdapter implements UnitLoadPort {

	private final UnitJpaRepository repository;

	@Override
	public Optional<Unit> findById(String unitId) {
		return repository.findById(unitId);
	}
}
