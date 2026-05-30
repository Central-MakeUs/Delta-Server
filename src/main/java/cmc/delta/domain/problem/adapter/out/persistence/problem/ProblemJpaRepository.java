package cmc.delta.domain.problem.adapter.out.persistence.problem;

import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.model.problem.Problem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProblemJpaRepository extends JpaRepository<Problem, Long>, ProblemRepositoryPort {

	boolean existsByScan_Id(Long scanId);

	boolean existsByOriginalStorageKey(String storageKey);

	@Query("""
			select p.originalStorageKey
			  from Problem p
			 where p.user.id = :userId
			   and p.originalStorageKey is not null
			   and p.originalStorageKey <> ''
		""")
	List<String> findOriginalStorageKeysByUserId(Long userId);

	Optional<Problem> findByScan_Id(Long scanId);

	long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

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

	@Override
	@Modifying
	@Query("update Problem p set p.viewCount = p.viewCount + 1 where p.id = :id")
	void incrementViewCount(Long id);

	@Override
	@Modifying
	@Query("update Problem p set p.aiSolutionCount = p.aiSolutionCount + 1 where p.id = :id")
	void incrementAiSolutionCount(Long id);
}
