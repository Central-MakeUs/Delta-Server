package cmc.delta.domain.curriculum.adapter.out.persistence.adapter;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.application.port.out.ProblemTypeRepositoryPort;
import cmc.delta.domain.curriculum.model.ProblemType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProblemTypeRepositoryAdapter implements ProblemTypeRepositoryPort {

	private final ProblemTypeJpaRepository problemTypeJpaRepository;

	@Override
	public List<ProblemType> findAllActiveForUser(Long userId) {
		return problemTypeJpaRepository.findAllActiveForUser(userId);
	}
}
