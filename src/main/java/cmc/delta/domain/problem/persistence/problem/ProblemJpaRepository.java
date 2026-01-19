package cmc.delta.domain.problem.persistence.problem;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.persistence.problem.query.ProblemQueryRepository;

public interface ProblemJpaRepository extends JpaRepository<Problem, Long> , ProblemQueryRepository {
	Optional<Problem> findByIdAndUserId(Long id, Long userId);
	boolean existsByScan_Id(Long scanId);
}
