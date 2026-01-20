package cmc.delta.domain.problem.adapter.out.persistence.problem;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import cmc.delta.domain.problem.model.problem.Problem;

public interface ProblemJpaRepository extends JpaRepository<Problem, Long> {
	Optional<Problem> findByIdAndUserId(Long id, Long userId);
	boolean existsByScan_Id(Long scanId);
}
