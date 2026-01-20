package cmc.delta.domain.curriculum.application.port.out;

import cmc.delta.domain.curriculum.model.UnitTypeMap;
import java.util.List;

public interface UnitTypeMapRepositoryPort {
	List<UnitTypeMap> findAllActiveForUser(Long userId);
}
