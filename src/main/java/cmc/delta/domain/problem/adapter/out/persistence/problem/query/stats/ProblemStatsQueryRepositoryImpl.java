package cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.model.enums.ProblemStatsSort;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemStatsQueryPort;
import cmc.delta.domain.problem.model.problem.QProblem;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats.dto.ProblemStatsCondition;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats.dto.ProblemTypeStatsRow;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats.dto.ProblemUnitStatsRow;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;

import cmc.delta.domain.problem.model.problem.QProblemTypeTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProblemStatsQueryRepositoryImpl implements ProblemStatsQueryPort {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<ProblemUnitStatsRow> findUnitStats(Long userId, ProblemStatsCondition condition) {
		QProblem problem = QProblem.problem;
		QUnit unit = QUnit.unit;
		QUnit subject = new QUnit("subject");

		BooleanBuilder where = new BooleanBuilder();
		where.and(problem.user.id.eq(userId));

		if (hasText(condition.subjectId())) {
			where.and(subject.id.eq(condition.subjectId()));
		}
		if (hasText(condition.unitId())) {
			where.and(unit.id.eq(condition.unitId()));
		}

		NumberExpression<Long> solvedCount = new CaseBuilder()
			.when(problem.completedAt.isNotNull()).then(1L)
			.otherwise(0L)
			.sum();

		NumberExpression<Long> unsolvedCount = new CaseBuilder()
			.when(problem.completedAt.isNull()).then(1L)
			.otherwise(0L)
			.sum();

		NumberExpression<Long> totalCount = problem.id.count();

		OrderSpecifier<?>[] orderBy = resolveUnitSort(condition.sort(), totalCount, subject, unit);

		return queryFactory
			.select(constructor(
				ProblemUnitStatsRow.class,
				subject.id,
				subject.name,
				unit.id,
				unit.name,
				solvedCount,
				unsolvedCount,
				totalCount
			))
			.from(problem)
			.join(problem.finalUnit, unit)
			.leftJoin(unit.parent, subject)
			.where(where)
			.groupBy(subject.id, subject.name, unit.id, unit.name)
			.orderBy(orderBy)
			.fetch();
	}

	@Override
	public List<ProblemTypeStatsRow> findTypeStats(Long userId, ProblemStatsCondition condition) {
		QProblemTypeTag tag = QProblemTypeTag.problemTypeTag;
		QProblem problem = QProblem.problem;
		QProblemType type = QProblemType.problemType;
		QUnit unit = QUnit.unit;
		QUnit subject = new QUnit("subject");

		BooleanBuilder where = new BooleanBuilder();
		where.and(problem.user.id.eq(userId));

		if (hasText(condition.subjectId())) {
			where.and(subject.id.eq(condition.subjectId()));
		}
		if (hasText(condition.unitId())) {
			where.and(unit.id.eq(condition.unitId()));
		}
		if (hasText(condition.typeId())) {
			where.and(type.id.eq(condition.typeId()));
		}

		NumberExpression<Long> solvedCount = new CaseBuilder()
			.when(problem.completedAt.isNotNull()).then(1L)
			.otherwise(0L)
			.sum();

		NumberExpression<Long> unsolvedCount = new CaseBuilder()
			.when(problem.completedAt.isNull()).then(1L)
			.otherwise(0L)
			.sum();

		NumberExpression<Long> totalCount = tag.id.problemId.count(); // tag row 기준 count

		OrderSpecifier<?>[] orderBy = resolveTypeSort(condition.sort(), totalCount, type);

		return queryFactory
			.select(constructor(
				ProblemTypeStatsRow.class,
				type.id,
				type.name,
				solvedCount,
				unsolvedCount,
				totalCount
			))
			.from(tag)
			.join(tag.problem, problem)
			.join(tag.type, type)
			.join(problem.finalUnit, unit)
			.leftJoin(unit.parent, subject)
			.where(where)
			.groupBy(type.id, type.name)
			.orderBy(orderBy)
			.fetch();
	}


	private OrderSpecifier<?>[] resolveUnitSort(
		ProblemStatsSort sort,
		NumberExpression<Long> totalCount,
		QUnit subject,
		QUnit unit
	) {
		OrderSpecifier<?> d1 = subject.name.asc();
		OrderSpecifier<?> d2 = unit.name.asc();

		if (sort == null || sort == ProblemStatsSort.DEFAULT) {
			return new OrderSpecifier<?>[]{ d1, d2 };
		}
		if (sort == ProblemStatsSort.MAX) {
			return new OrderSpecifier<?>[]{ totalCount.desc(), d1, d2 };
		}
		if (sort == ProblemStatsSort.MIN) {
			return new OrderSpecifier<?>[]{ totalCount.asc(), d1, d2 };
		}
		return new OrderSpecifier<?>[]{ d1, d2 };
	}

	private OrderSpecifier<?>[] resolveTypeSort(
		ProblemStatsSort sort,
		NumberExpression<Long> totalCount,
		QProblemType type
	) {
		OrderSpecifier<?> d1 = type.name.asc();

		if (sort == null || sort == ProblemStatsSort.DEFAULT) {
			return new OrderSpecifier<?>[]{ d1 };
		}
		if (sort == ProblemStatsSort.MAX) {
			return new OrderSpecifier<?>[]{ totalCount.desc(), d1 };
		}
		if (sort == ProblemStatsSort.MIN) {
			return new OrderSpecifier<?>[]{ totalCount.asc(), d1 };
		}
		return new OrderSpecifier<?>[]{ d1 };
	}

	private boolean hasText(String v) {
		return v != null && !v.trim().isEmpty();
	}
}
