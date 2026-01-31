package cmc.delta.domain.curriculum.application.port.out;

import cmc.delta.domain.curriculum.model.ProblemType;
import java.util.List;
import java.util.Optional;

public interface ProblemTypeRepositoryPort {
	List<ProblemType> findAllActiveForUser(Long userId);

	List<ProblemType> findAllForUser(Long userId);

	Optional<ProblemType> findOwnedCustomById(Long userId, String typeId);

	// 사용자 소유의 커스텀 유형을 이름으로 조회(존재 여부 및 활성 상태 확인용)
	Optional<ProblemType> findOwnedCustomByUserIdAndName(Long userId, String name);

	boolean existsCustomByUserIdAndName(Long userId, String name);

	int findMaxSortOrderVisibleForUser(Long userId);

	ProblemType save(ProblemType type);
}
