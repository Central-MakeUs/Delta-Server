package cmc.delta.domain.problem.adapter.out.persistence.problem;

import cmc.delta.domain.problem.application.port.out.problem.ProblemAiSolutionTaskRepositoryPort;
import cmc.delta.domain.problem.model.enums.ProblemAiSolutionStatus;
import cmc.delta.domain.problem.model.problem.ProblemAiSolutionTask;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select t
			  from ProblemAiSolutionTask t
			 where t.status = :status
			   and (t.nextRetryAt is null or t.nextRetryAt <= :now)
			 order by t.requestedAt asc
		""")
	List<ProblemAiSolutionTask> findPendingCandidatesForUpdate(
		@Param("status") ProblemAiSolutionStatus status,
		@Param("now") LocalDateTime now,
		Pageable pageRequest);

	void deleteByProblem_Id(Long problemId);

	@Override
	default Optional<ProblemAiSolutionTask> findByProblemId(Long problemId) {
		return findByProblem_Id(problemId);
	}

	@Override
	default Optional<ProblemAiSolutionTask> findByProblemIdForUpdate(Long problemId) {
		return findByProblemIdWithLock(problemId);
	}

	@Override
	default void deleteByProblemId(Long problemId) {
		deleteByProblem_Id(problemId);
	}

	@Override
	default Optional<ProblemAiSolutionTask> findNextPendingForUpdate(LocalDateTime now) {
		List<ProblemAiSolutionTask> candidates = findPendingCandidatesForUpdate(
			ProblemAiSolutionStatus.PENDING,
			now,
			PageRequest.of(0, 1));
		if (candidates.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(candidates.get(0));
	}
}
