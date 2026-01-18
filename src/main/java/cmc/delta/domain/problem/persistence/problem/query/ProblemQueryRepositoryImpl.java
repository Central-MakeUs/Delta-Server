package cmc.delta.domain.problem.persistence.problem.query;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.model.asset.QAsset;
import cmc.delta.domain.problem.model.enums.AssetType;
import cmc.delta.domain.problem.model.problem.QProblem;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListCondition;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListRow;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProblemQueryRepositoryImpl implements ProblemQueryRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<ProblemListRow> findMyProblemList(Long userId, ProblemListCondition condition, Pageable pageable) {
		QProblem problem = QProblem.problem;
		QUnit unit = QUnit.unit;
		QUnit subject = new QUnit("subject");
		QProblemType type = QProblemType.problemType;
		QAsset asset = QAsset.asset;

		BooleanBuilder where = new BooleanBuilder();
		where.and(problem.user.id.eq(userId));

		if (hasText(condition.unitId())) {
			where.and(unit.id.eq(condition.unitId()));
		}
		if (hasText(condition.typeId())) {
			where.and(type.id.eq(condition.typeId()));
		}
		if (hasText(condition.subjectId())) {
			where.and(subject.id.eq(condition.subjectId()));
		}

		JPAQuery<ProblemListRow> contentQuery = queryFactory
			.select(constructor(
				ProblemListRow.class,
				problem.id,
				subject.id,
				subject.name,
				unit.id,
				unit.name,
				type.id,
				type.name,
				asset.id,
				asset.storageKey,
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
			.where(where);

		applySort(contentQuery, condition);

		List<ProblemListRow> content = contentQuery
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		Long total = queryFactory
			.select(problem.count())
			.from(problem)
			.join(problem.finalUnit, unit)
			.leftJoin(unit.parent, subject)
			.join(problem.finalType, type)
			.where(where)
			.fetchOne();

		long totalElements = 0L;
		if (total != null) {
			totalElements = total.longValue();
		}

		return new PageImpl<ProblemListRow>(content, pageable, totalElements);
	}

	private void applySort(JPAQuery<ProblemListRow> query, ProblemListCondition condition) {
		QProblem problem = QProblem.problem;

		if (condition.sort() != null && condition.sort().name().equals("OLDEST")) {
			query.orderBy(problem.createdAt.asc());
			return;
		}
		query.orderBy(problem.createdAt.desc());
	}

	private boolean hasText(String value) {
		if (value == null) {
			return false;
		}
		return !value.trim().isEmpty();
	}
}
