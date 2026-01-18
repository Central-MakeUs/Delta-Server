package cmc.delta.domain.problem.persistence.scan;

import org.springframework.data.jpa.repository.JpaRepository;

import cmc.delta.domain.problem.model.problem.Problem;

public interface ProblemJpaRepository extends JpaRepository<Problem, Long> {
	boolean existsByScan_Id(Long scanId);
}
