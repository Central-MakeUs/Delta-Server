package cmc.delta.domain.problem.adapter.out.persistence.problem;

import cmc.delta.domain.problem.application.port.out.problem.ProblemAiSolutionTaskRepositoryPort;
import cmc.delta.domain.problem.model.problem.ProblemAiSolutionTask;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ProblemAiSolutionTaskJpaRepository
	extends JpaRepository<ProblemAiSolutionTask, Long>, ProblemAiSolutionTaskRepositoryPort {

	Optional<ProblemAiSolutionTask> findByProblem_Id(Long problemId);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select t
			  from ProblemAiSolutionTask t
			 where t.problem.id = :problemId
		""")
	Optional<ProblemAiSolutionTask> findByProblemIdWithLock(Long problemId);

	@Override
	default Optional<ProblemAiSolutionTask> findByProblemId(Long problemId) {
		return findByProblem_Id(problemId);
	}

	@Override
	default Optional<ProblemAiSolutionTask> findByProblemIdForUpdate(Long problemId) {
		return findByProblemIdWithLock(problemId);
	}
}
