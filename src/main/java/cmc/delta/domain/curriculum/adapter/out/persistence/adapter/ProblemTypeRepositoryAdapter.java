package cmc.delta.domain.curriculum.adapter.out.persistence.adapter;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.application.port.out.ProblemTypeRepositoryPort;
import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.global.cache.CacheNames;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProblemTypeRepositoryAdapter implements ProblemTypeRepositoryPort {

	private final ProblemTypeJpaRepository problemTypeJpaRepository;

	@Override
	public List<ProblemType> findAllActiveForUser(Long userId) {
		return problemTypeJpaRepository.findAllActiveForUser(userId);
	}

	@Override
	public List<ProblemType> findAllForUser(Long userId) {
		return problemTypeJpaRepository.findAllForUser(userId);
	}

	@Override
	public List<ProblemType> findAllActiveFixed() {
		return problemTypeJpaRepository.findAllActiveFixed();
	}

	@Override
	@Cacheable(cacheNames = CacheNames.CUSTOM_PROBLEM_TYPES, key = "#userId")
	public List<ProblemType> findActiveCustomByUserId(Long userId) {
		return problemTypeJpaRepository.findActiveCustomByUserId(userId);
	}

	@Override
	@CacheEvict(cacheNames = CacheNames.CUSTOM_PROBLEM_TYPES, key = "#type.createdByUser.id")
	public ProblemType save(ProblemType type) {
		return problemTypeJpaRepository.save(type);
	}

	@Override
	public Optional<ProblemType> findOwnedCustomById(Long userId, String typeId) {
		return problemTypeJpaRepository.findOwnedCustomById(userId, typeId);
	}

	@Override
	public Optional<ProblemType> findOwnedCustomByUserIdAndName(Long userId, String name) {
		return problemTypeJpaRepository.findOwnedCustomByUserIdAndName(userId, name);
	}

	@Override
	public boolean existsCustomByUserIdAndName(Long userId, String name) {
		return problemTypeJpaRepository.existsByCreatedByUserIdAndCustomTrueAndName(userId, name);
	}

	@Override
	public int findMaxSortOrderVisibleForUser(Long userId) {
		return problemTypeJpaRepository.findMaxSortOrderVisibleForUser(userId);
	}
}
