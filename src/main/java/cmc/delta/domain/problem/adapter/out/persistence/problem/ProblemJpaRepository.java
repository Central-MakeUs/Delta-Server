package cmc.delta.domain.problem.adapter.out.persistence.problem;

import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.model.problem.Problem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProblemJpaRepository extends JpaRepository<Problem, Long>, ProblemRepositoryPort {

	boolean existsByScan_Id(Long scanId);

	boolean existsByOriginalStorageKey(String storageKey);

	@Query("""
			select p
			  from Problem p
			 where (p.originalStorageKey is null or p.originalStorageKey = '')
			   and p.scan is not null
			 order by p.id asc
		""")
	List<Problem> findKeyBackfillCandidates(Pageable pageable);

	@Query("""
			select p.originalStorageKey
			  from Problem p
			 where p.user.id = :userId
			   and p.originalStorageKey is not null
			   and p.originalStorageKey <> ''
		""")
	List<String> findOriginalStorageKeysByUserId(Long userId);

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
