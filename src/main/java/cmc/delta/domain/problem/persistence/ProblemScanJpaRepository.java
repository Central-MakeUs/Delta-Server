package cmc.delta.domain.problem.persistence;

import cmc.delta.domain.problem.model.ProblemScan;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * QueryDSL로 변경 예정
 */
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

	  u.id as predictedUnitId,
	  u.name as predictedUnitName,
	  t.id as predictedTypeId,
	  t.name as predictedTypeName,
	  s.confidence as confidence,
	  s.needsReview as needsReview,
	  s.aiUnitCandidatesJson as aiUnitCandidatesJson,
	  s.aiTypeCandidatesJson as aiTypeCandidatesJson,
	  s.aiDraftJson as aiDraftJson,

	  s.createdAt as createdAt,
	  s.ocrCompletedAt as ocrCompletedAt,
	  s.aiCompletedAt as aiCompletedAt,
	  s.failReason as failReason

	from ProblemScan s
	left join Asset a
	  on a.scan = s
	 and a.assetType = cmc.delta.domain.problem.model.enums.AssetType.ORIGINAL
	left join s.predictedUnit u
	left join s.predictedType t
	where s.id = :scanId
	  and s.user.id = :userId
""")
	Optional<ProblemScanDetailProjection> findOwnedDetail(Long scanId, Long userId);


	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select s
		  from ProblemScan s
		 where s.id = :scanId
		   and s.user.id = :userId
	""")
	Optional<ProblemScan> findOwnedByForUpdate(
		@Param("scanId") Long scanId,
		@Param("userId") Long userId
	);

	// ProblemScanJpaRepository

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

}
