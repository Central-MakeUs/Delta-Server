package cmc.delta.domain.problem.adapter.out.persistence.scan.query;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.application.port.out.scan.query.ScanQueryPort;
import cmc.delta.domain.problem.adapter.out.persistence.scan.query.dto.ScanListRow;
import cmc.delta.domain.problem.adapter.out.persistence.scan.query.projection.ScanDetailProjection;
import cmc.delta.domain.problem.model.asset.QAsset;
import cmc.delta.domain.problem.model.enums.AssetType;
import cmc.delta.domain.problem.model.scan.QProblemScan;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ScanQueryAdapter implements ScanQueryPort {

	private final JPAQueryFactory queryFactory;
	private final ScanDetailRepository scanDetailRepository;

	@Override
	public Optional<ScanListRow> findListRow(Long userId, Long scanId) {
		QProblemScan scan = QProblemScan.problemScan;
		QAsset asset = QAsset.asset;

		QUnit unit = new QUnit("unit");
		QProblemType type = new QProblemType("type");

		ScanListRow row = queryFactory
			.select(constructor(
				ScanListRow.class,
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

	@Override
	public Optional<ScanDetailProjection> findDetail(Long userId, Long scanId) {
		return scanDetailRepository.findOwnedDetail(scanId, userId);
	}
}
