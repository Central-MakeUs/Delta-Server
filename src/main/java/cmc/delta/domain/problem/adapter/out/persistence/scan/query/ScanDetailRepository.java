package cmc.delta.domain.problem.adapter.out.persistence.scan.query;

import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.problem.adapter.out.persistence.scan.query.projection.ScanDetailProjection;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ScanDetailRepository extends Repository<ProblemScan, Long> {

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
	Optional<ScanDetailProjection> findOwnedDetail(
		@Param("scanId") Long scanId,
		@Param("userId") Long userId
	);
}
