package cmc.delta.domain.curriculum.application.port.out;

import cmc.delta.domain.curriculum.model.ProblemType;
import java.util.List;
import java.util.Optional;

public interface ProblemTypeLoadPort {
	Optional<ProblemType> findById(String typeId);

	Optional<ProblemType> findActiveVisibleById(Long userId, String typeId);

	List<ProblemType> findActiveVisibleByIds(Long userId, List<String> typeIds);
}
