package cmc.delta.domain.problem.persistence.scan.worker;

import cmc.delta.domain.problem.model.scan.ProblemScan;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ScanWorkRepository extends Repository<ProblemScan, Long> {

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(
		value = """
			update problem_scan
			   set locked_at = :lockedAt,
			       lock_owner = :lockOwner,
			       lock_token = :lockToken
			 where status = 'UPLOADED'
			   and (locked_at is null or locked_at <= :staleBefore)
			   and (next_retry_at is null or next_retry_at <= :now)
			 order by created_at asc, id asc
			 limit :limit
			""",
		nativeQuery = true
	)
	int claimOcrCandidates(
		@Param("now") LocalDateTime now,
		@Param("staleBefore") LocalDateTime staleBefore,
		@Param("lockOwner") String lockOwner,
		@Param("lockToken") String lockToken,
		@Param("lockedAt") LocalDateTime lockedAt,
		@Param("limit") int limit
	);

	@Query(
		value = """
			select id
			  from problem_scan
			 where lock_owner = :lockOwner
			   and lock_token = :lockToken
			 order by created_at asc, id asc
			 limit :limit
			""",
		nativeQuery = true
	)
	List<Long> findClaimedOcrIds(
		@Param("lockOwner") String lockOwner,
		@Param("lockToken") String lockToken,
		@Param("limit") int limit
	);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(
		value = """
			update problem_scan
			   set locked_at = :lockedAt,
			       lock_owner = :lockOwner,
			       lock_token = :lockToken
			 where status = 'OCR_DONE'
			   and (locked_at is null or locked_at <= :staleBefore)
			   and (next_retry_at is null or next_retry_at <= :now)
			 order by created_at asc, id asc
			 limit :limit
			""",
		nativeQuery = true
	)
	int claimAiCandidates(
		@Param("now") LocalDateTime now,
		@Param("staleBefore") LocalDateTime staleBefore,
		@Param("lockOwner") String lockOwner,
		@Param("lockToken") String lockToken,
		@Param("lockedAt") LocalDateTime lockedAt,
		@Param("limit") int limit
	);

	@Query(
		value = """
			select id
			  from problem_scan
			 where lock_owner = :lockOwner
			   and lock_token = :lockToken
			 order by created_at asc, id asc
			 limit :limit
			""",
		nativeQuery = true
	)
	List<Long> findClaimedAiIds(
		@Param("lockOwner") String lockOwner,
		@Param("lockToken") String lockToken,
		@Param("limit") int limit
	);

	@Query(
		value = """
			select 1
			  from problem_scan
			 where id = :scanId
			   and lock_owner = :lockOwner
			   and lock_token = :lockToken
			 limit 1
			""",
		nativeQuery = true
	)
	Integer existsLockedBy(
		@Param("scanId") Long scanId,
		@Param("lockOwner") String lockOwner,
		@Param("lockToken") String lockToken
	);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(
		value = """
			update problem_scan
			   set locked_at = null,
			       lock_owner = null,
			       lock_token = null
			 where id = :scanId
			   and lock_owner = :lockOwner
			   and lock_token = :lockToken
			""",
		nativeQuery = true
	)
	int unlock(
		@Param("scanId") Long scanId,
		@Param("lockOwner") String lockOwner,
		@Param("lockToken") String lockToken
	);

	@Query(
		value = """
		select count(*)
		  from problem_scan
		 where status = 'UPLOADED'
		   and (locked_at is null or locked_at <= :staleBefore)
		   and (next_retry_at is null or next_retry_at <= :now)
		""",
		nativeQuery = true
	)
	long countOcrBacklog(
		@Param("now") LocalDateTime now,
		@Param("staleBefore") LocalDateTime staleBefore
	);

	@Query(
		value = """
		select count(*)
		  from problem_scan
		 where status = 'OCR_DONE'
		   and (locked_at is null or locked_at <= :staleBefore)
		   and (next_retry_at is null or next_retry_at <= :now)
		""",
		nativeQuery = true
	)
	long countAiBacklog(
		@Param("now") LocalDateTime now,
		@Param("staleBefore") LocalDateTime staleBefore
	);

	@Query(
		value = """
	select count(*)
	  from problem_scan
	 where status = 'FAILED'
	   and updated_at >= :from
	""",
		nativeQuery = true
	)
	long countFailedSince(@Param("from") LocalDateTime from);

	long countByOcrCompletedAtGreaterThanEqual(LocalDateTime from);

	long countByAiCompletedAtGreaterThanEqual(LocalDateTime from);
}
