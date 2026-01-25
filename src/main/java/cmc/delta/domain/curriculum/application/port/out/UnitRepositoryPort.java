package cmc.delta.domain.curriculum.application.port.out;

import cmc.delta.domain.curriculum.model.Unit;
import java.util.List;

public interface UnitRepositoryPort {
	List<Unit> findAllRootUnitsActive();

	List<Unit> findAllChildUnitsActive();
}
