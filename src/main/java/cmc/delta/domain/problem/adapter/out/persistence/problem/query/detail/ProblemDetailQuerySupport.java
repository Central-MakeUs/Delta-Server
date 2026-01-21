package cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail.dto.ProblemDetailRow;
import cmc.delta.domain.problem.model.asset.QAsset;
import cmc.delta.domain.problem.model.enums.AssetType;
import cmc.delta.domain.problem.model.problem.QProblem;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemDetailQuerySupport {

	private final JPAQueryFactory queryFactory;

	public Optional<ProblemDetailRow> findMyProblemDetail(Long userId, Long problemId) {
		QProblem problem = QProblem.problem;
		QUnit unit = QUnit.unit;
		QUnit subject = new QUnit("subject");
		QProblemType type = QProblemType.problemType;
		QAsset asset = QAsset.asset;

		ProblemDetailRow row = queryFactory
			.select(constructor(
				ProblemDetailRow.class,
				problem.id,

				subject.id,
				subject.name,

				unit.id,
				unit.name,

				type.id,
				type.name,

				asset.id,
				asset.storageKey,

				problem.answerFormat,
				problem.answerChoiceNo,
				problem.answerValue,
				problem.solutionText,

				problem.completedAt,
				problem.createdAt
			))
			.from(problem)
			.join(problem.finalUnit, unit)
			.leftJoin(unit.parent, subject)
			.join(problem.finalType, type)
			.join(asset).on(
				asset.scan.id.eq(problem.scan.id)
					.and(asset.assetType.eq(AssetType.ORIGINAL))
			)
			.where(
				problem.user.id.eq(userId),
				problem.id.eq(problemId)
			)
			.fetchOne();

		return Optional.ofNullable(row);
	}
}
