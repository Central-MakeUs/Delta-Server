package cmc.delta.domain.problem.persistence;

import cmc.delta.domain.problem.model.ProblemScan;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProblemScanJpaRepository extends JpaRepository<ProblemScan, Long> {

	@Query("""
		select s.id
		from ProblemScan s
		where s.status = cmc.delta.domain.problem.model.enums.ScanStatus.UPLOADED
		  and s.lockedAt is null
		  and (s.nextRetryAt is null or s.nextRetryAt <= :now)
		order by s.createdAt asc, s.id asc
		""")
	Optional<Long> findNextOcrCandidateId(@Param("now") LocalDateTime now);

	/**
	 * OCR 후보만 락을 잡는다 (상태/재시도조건 포함).
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update ProblemScan s
		   set s.lockedAt = :now,
		       s.lockOwner = :lockOwner
		 where s.id = :scanId
		   and s.status = cmc.delta.domain.problem.model.enums.ScanStatus.UPLOADED
		   and s.lockedAt is null
		   and (s.nextRetryAt is null or s.nextRetryAt <= :now)
		""")
	int tryLockOcrCandidate(
		@Param("scanId") Long scanId,
		@Param("lockOwner") String lockOwner,
		@Param("now") LocalDateTime now
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update ProblemScan s
		   set s.lockedAt = null,
		       s.lockOwner = null
		 where s.id = :scanId
		   and s.lockOwner = :lockOwner
		""")
	int unlock(@Param("scanId") Long scanId, @Param("lockOwner") String lockOwner);

	@Query("""
			select s
			from ProblemScan s
			join s.user u
			where s.id = :scanId
			  and u.id = :userId
		""")
	Optional<ProblemScan> findOwnedBy(@Param("scanId") Long scanId, @Param("userId") Long userId);

	// ProblemScanJpaRepository

	@Query("""
			select count(s)
			from ProblemScan s
			where s.status = cmc.delta.domain.problem.model.enums.ScanStatus.UPLOADED
			  and s.lockedAt is null
			  and (s.nextRetryAt is null or s.nextRetryAt <= :now)
		""")
	long countOcrCandidates(@Param("now") LocalDateTime now);
}
