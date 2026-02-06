package cmc.delta.domain.problem.adapter.out.persistence.problem;

import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.model.problem.Problem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemJpaRepository extends JpaRepository<Problem, Long>, ProblemRepositoryPort {

	boolean existsByScan_Id(Long scanId);

	Optional<Problem> findByScan_Id(Long scanId);

	@Override
	Optional<Problem> findByIdAndUserId(Long id, Long userId);

	@Override
	default boolean existsByScanId(Long scanId) {
		return existsByScan_Id(scanId);
	}

	@Override
	default Optional<Problem> findByScanId(Long scanId) {
		return findByScan_Id(scanId);
	}
}
