package cmc.delta.domain.curriculum.application.port.out;

import cmc.delta.domain.curriculum.model.ProblemType;
import java.util.List;

public interface ProblemTypeRepositoryPort {
	List<ProblemType> findAllActiveForUser(Long userId);
}
