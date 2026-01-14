package cmc.delta.domain.problem.persistence;

import cmc.delta.domain.problem.model.ProblemScan;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProblemScanJpaRepository extends JpaRepository<ProblemScan, Long> {

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
	@Query("""
		update ProblemScan s
		   set s.lockedAt = null,
		       s.lockOwner = null,
		       s.lockToken = null
		 where s.id = :scanId
		   and s.lockOwner = :lockOwner
		""")
	int unlock(@Param("scanId") Long scanId, @Param("lockOwner") String lockOwner);

	@Override
	Optional<ProblemScan> findById(Long scanId);

	@Query("""
	select
	  s.id as scanId,
	  s.status as status,
	  s.hasFigure as hasFigure,
	  s.renderMode as renderMode,

	  a.id as assetId,
	  a.storageKey as storageKey,
	  a.width as width,
	  a.height as height,

	  s.ocrPlainText as ocrPlainText,
	  s.aiProblemLatex as aiProblemLatex,
	  s.aiSolutionLatex as aiSolutionLatex,

	  s.createdAt as createdAt,
	  s.ocrCompletedAt as ocrCompletedAt,
	  s.aiCompletedAt as aiCompletedAt,
	  s.failReason as failReason
	from ProblemScan s
	left join Asset a
	  on a.scan = s
	 and a.assetType = cmc.delta.domain.problem.model.enums.AssetType.ORIGINAL
	where s.id = :scanId
	  and s.user.id = :userId
""")
	Optional<ProblemScanDetailProjection> findOwnedDetail(
		@Param("scanId") Long scanId,
		@Param("userId") Long userId
	);
}
