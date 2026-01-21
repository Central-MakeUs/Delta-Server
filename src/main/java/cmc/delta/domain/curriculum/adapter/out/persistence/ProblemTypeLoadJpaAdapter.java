package cmc.delta.domain.curriculum.adapter.out.persistence;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.application.port.out.ProblemTypeLoadPort;
import cmc.delta.domain.curriculum.model.ProblemType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemTypeLoadJpaAdapter implements ProblemTypeLoadPort {

	private final ProblemTypeJpaRepository repository;

	@Override
	public Optional<ProblemType> findById(String typeId) {
		return repository.findById(typeId);
	}
}
