package cmc.delta.domain.problem.persistence.problem;

import org.springframework.data.jpa.repository.JpaRepository;

import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.persistence.problem.query.ProblemQueryRepository;

public interface ProblemJpaRepository extends JpaRepository<Problem, Long> , ProblemQueryRepository {
	boolean existsByScan_Id(Long scanId);
}
