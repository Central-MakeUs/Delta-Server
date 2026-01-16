package cmc.delta.domain.problem.persistence.scan;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.model.asset.QAsset;
import cmc.delta.domain.problem.model.enums.AssetType;
import cmc.delta.domain.problem.model.scan.QProblemScan;
import cmc.delta.domain.problem.persistence.scan.dto.ProblemScanSummaryRow;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProblemScanSummaryRepositoryImpl implements ProblemScanSummaryRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<ProblemScanSummaryRow> findSummaryRow(Long userId, Long scanId) {
		QProblemScan scan = QProblemScan.problemScan;
		QAsset asset = QAsset.asset;

		QUnit unit = new QUnit("unit");
		QProblemType type = new QProblemType("type");

		ProblemScanSummaryRow row = queryFactory
			.select(constructor(
				ProblemScanSummaryRow.class,
				scan.id,
				scan.user.id,
				scan.status,

				asset.id,
				asset.storageKey,

				unit.id,
				unit.name,

				type.id,
				type.name,

				scan.needsReview
			))
			.from(scan)
			.join(asset).on(
				asset.scan.id.eq(scan.id)
					.and(asset.assetType.eq(AssetType.ORIGINAL))
			)
			.leftJoin(scan.predictedUnit, unit)
			.leftJoin(scan.predictedType, type)
			.where(
				scan.id.eq(scanId),
				scan.user.id.eq(userId)
			)
			.fetchOne();

		return Optional.ofNullable(row);
	}
}
