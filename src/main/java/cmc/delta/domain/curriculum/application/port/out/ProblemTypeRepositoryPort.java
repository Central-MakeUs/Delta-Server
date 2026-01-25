package cmc.delta.domain.curriculum.application.port.out;

import cmc.delta.domain.curriculum.model.ProblemType;
import java.util.List;
import java.util.Optional;

public interface ProblemTypeRepositoryPort {
	List<ProblemType> findAllActiveForUser(Long userId);

	List<ProblemType> findAllForUser(Long userId);

	Optional<ProblemType> findOwnedCustomById(Long userId, String typeId);

	boolean existsCustomByUserIdAndName(Long userId, String name);

	int findMaxSortOrderVisibleForUser(Long userId);

	ProblemType save(ProblemType type);
}
