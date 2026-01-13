package cmc.delta.domain.problem.persistence;

import cmc.delta.domain.problem.model.ProblemScan;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProblemScanJpaRepository extends JpaRepository<ProblemScan, Long> {

	@Query(value = """
		select id
		from problem_scan
		where status = 'UPLOADED'
		  and locked_at is null
		  and (next_retry_at is null or next_retry_at <= :now)
		order by created_at asc
		limit 1
		""", nativeQuery = true)
	Optional<Long> findNextOcrCandidateId(@Param("now") LocalDateTime now);

	@Modifying
	@Query(value = """
		update problem_scan
		   set locked_at = :now,
		       lock_owner = :owner
		 where id = :id
		   and locked_at is null
		""", nativeQuery = true)
	int tryLock(@Param("id") Long id, @Param("owner") String owner, @Param("now") LocalDateTime now);

	@Modifying
	@Query(value = """
		update problem_scan
		   set locked_at = null,
		       lock_owner = null
		 where id = :id
		   and lock_owner = :owner
		""", nativeQuery = true)
	int unlock(@Param("id") Long id, @Param("owner") String owner);
}
