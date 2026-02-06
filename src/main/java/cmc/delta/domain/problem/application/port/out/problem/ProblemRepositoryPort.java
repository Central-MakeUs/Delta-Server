package cmc.delta.domain.problem.application.port.out.problem;

import cmc.delta.domain.problem.model.problem.Problem;
import java.util.Optional;

public interface ProblemRepositoryPort {

	Problem save(Problem problem);

	Optional<Problem> findById(Long id);

	Optional<Problem> findByIdAndUserId(Long id, Long userId);

	Optional<Problem> findByScanId(Long scanId);

	boolean existsByScanId(Long scanId);
}
