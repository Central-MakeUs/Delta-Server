package cmc.delta.domain.curriculum.application.port.out;

import cmc.delta.domain.curriculum.model.Unit;
import java.util.Optional;

public interface UnitLoadPort {
	Optional<Unit> findById(String unitId);
}
