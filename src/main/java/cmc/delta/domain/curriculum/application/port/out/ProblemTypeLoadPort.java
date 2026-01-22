package cmc.delta.domain.curriculum.application.port.out;

import cmc.delta.domain.curriculum.model.ProblemType;
import java.util.Optional;

public interface ProblemTypeLoadPort {
	Optional<ProblemType> findById(String typeId);
}
