package cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemStatsCondition;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemStatsQueryPort;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemMonthlyProgressRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeStatsRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemUnitStatsRow;
import cmc.delta.domain.problem.model.enums.ProblemStatsSort;
import cmc.delta.domain.problem.model.problem.QProblem;
import cmc.delta.domain.problem.model.problem.QProblemTypeTag;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProblemStatsQueryRepositoryImpl implements ProblemStatsQueryPort {

	private static final long COUNT_ONE = 1L;
	private static final long COUNT_ZERO = 0L;

	private final JPAQueryFactory queryFactory;

	@Override
	public List<ProblemUnitStatsRow> findUnitStats(Long userId, ProblemStatsCondition condition) {
		QProblem problem = QProblem.problem;
		QUnit unit = QUnit.unit;
		QUnit subject = new QUnit("subject");

		BooleanBuilder where = buildUnitStatsWhere(userId, condition, problem, unit, subject);
		NumberExpression<Long> totalCount = problem.id.count();
		OrderSpecifier<?>[] orderBy = resolveUnitSort(condition.sort(), totalCount, subject, unit);

		return fetchUnitStats(problem, unit, subject, totalCount, where, orderBy);
	}

	private BooleanBuilder buildUnitStatsWhere(
		Long userId,
		ProblemStatsCondition condition,
		QProblem problem,
		QUnit unit,
		QUnit subject) {
		BooleanBuilder where = new BooleanBuilder();
		where.and(problem.user.id.eq(userId));

		if (hasText(condition.subjectId())) {
			where.and(subject.id.eq(condition.subjectId()));
		}
		if (hasText(condition.unitId())) {
			where.and(unit.id.eq(condition.unitId()));
		}
		return where;
	}

	private List<ProblemUnitStatsRow> fetchUnitStats(
		QProblem problem,
		QUnit unit,
		QUnit subject,
		NumberExpression<Long> totalCount,
		BooleanBuilder where,
		OrderSpecifier<?>[] orderBy) {
		return queryFactory
			.select(unitStatsProjection(problem, unit, subject, totalCount))
			.from(problem)
			.join(problem.finalUnit, unit)
			.leftJoin(unit.parent, subject)
			.where(where)
			.groupBy(subject.id, subject.name, unit.id, unit.name)
			.orderBy(orderBy)
			.fetch();
	}

	private ConstructorExpression<ProblemUnitStatsRow> unitStatsProjection(
		QProblem problem,
		QUnit unit,
		QUnit subject,
		NumberExpression<Long> totalCount) {
		return constructor(
			ProblemUnitStatsRow.class,
			subject.id,
			subject.name,
			unit.id,
			unit.name,
			solvedCount(problem),
			unsolvedCount(problem),
			totalCount);
	}

	@Override
	public List<ProblemTypeStatsRow> findTypeStats(Long userId, ProblemStatsCondition condition) {
		QProblemTypeTag tag = QProblemTypeTag.problemTypeTag;
		QProblem problem = QProblem.problem;
		QProblemType type = QProblemType.problemType;
		QUnit unit = QUnit.unit;
		QUnit subject = new QUnit("subject");

		BooleanBuilder where = buildTypeStatsWhere(userId, condition, problem, type, unit, subject);
		NumberExpression<Long> totalCount = tag.id.problemId.count(); // tag row 기준 count
		OrderSpecifier<?>[] orderBy = resolveTypeSort(condition.sort(), totalCount, type);

		return fetchTypeStats(tag, problem, type, unit, subject, totalCount, where, orderBy);
	}

	private BooleanBuilder buildTypeStatsWhere(
		Long userId,
		ProblemStatsCondition condition,
		QProblem problem,
		QProblemType type,
		QUnit unit,
		QUnit subject) {
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
		return where;
	}

	private List<ProblemTypeStatsRow> fetchTypeStats(
		QProblemTypeTag tag,
		QProblem problem,
		QProblemType type,
		QUnit unit,
		QUnit subject,
		NumberExpression<Long> totalCount,
		BooleanBuilder where,
		OrderSpecifier<?>[] orderBy) {
		return queryFactory
			.select(typeStatsProjection(problem, type, totalCount))
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

	private ConstructorExpression<ProblemTypeStatsRow> typeStatsProjection(
		QProblem problem,
		QProblemType type,
		NumberExpression<Long> totalCount) {
		return constructor(
			ProblemTypeStatsRow.class,
			type.id,
			type.name,
			solvedCount(problem),
			unsolvedCount(problem),
			totalCount);
	}

	@Override
	public ProblemMonthlyProgressRow findMonthlyProgress(Long userId, LocalDateTime fromInclusive,
		LocalDateTime toExclusive) {
		QProblem problem = QProblem.problem;

		BooleanBuilder where = buildMonthlyProgressWhere(userId, fromInclusive, toExclusive, problem);
		ProblemMonthlyProgressRow row = fetchMonthlyProgress(problem, where);

		if (row == null) {
			return new ProblemMonthlyProgressRow(COUNT_ZERO, COUNT_ZERO, COUNT_ZERO);
		}
		return row;
	}

	private BooleanBuilder buildMonthlyProgressWhere(
		Long userId,
		LocalDateTime fromInclusive,
		LocalDateTime toExclusive,
		QProblem problem) {
		BooleanBuilder where = new BooleanBuilder();
		where.and(problem.user.id.eq(userId));
		where.and(problem.createdAt.goe(fromInclusive));
		where.and(problem.createdAt.lt(toExclusive));
		return where;
	}

	private ProblemMonthlyProgressRow fetchMonthlyProgress(QProblem problem, BooleanBuilder where) {
		return queryFactory
			.select(constructor(
				ProblemMonthlyProgressRow.class,
				problem.id.count(),
				solvedCount(problem),
				unsolvedCount(problem)))
			.from(problem)
			.where(where)
			.fetchOne();
	}

	private NumberExpression<Long> solvedCount(QProblem problem) {
		return new CaseBuilder()
			.when(problem.completedAt.isNotNull()).then(COUNT_ONE)
			.otherwise(COUNT_ZERO)
			.sum();
	}

	private NumberExpression<Long> unsolvedCount(QProblem problem) {
		return new CaseBuilder()
			.when(problem.completedAt.isNull()).then(COUNT_ONE)
			.otherwise(COUNT_ZERO)
			.sum();
	}

	private OrderSpecifier<?>[] resolveUnitSort(
		ProblemStatsSort sort,
		NumberExpression<Long> totalCount,
		QUnit subject,
		QUnit unit) {
		return resolveSort(sort, totalCount, subject.name.asc(), unit.name.asc());
	}

	private OrderSpecifier<?>[] resolveTypeSort(
		ProblemStatsSort sort,
		NumberExpression<Long> totalCount,
		QProblemType type) {
		return resolveSort(sort, totalCount, type.name.asc());
	}

	private OrderSpecifier<?>[] resolveSort(
		ProblemStatsSort sort,
		NumberExpression<Long> totalCount,
		OrderSpecifier<?>... defaultOrder) {
		if (sort == ProblemStatsSort.MAX) {
			return prependOrder(totalCount.desc(), defaultOrder);
		}
		if (sort == ProblemStatsSort.MIN) {
			return prependOrder(totalCount.asc(), defaultOrder);
		}
		return defaultOrder;
	}

	private OrderSpecifier<?>[] prependOrder(OrderSpecifier<?> first, OrderSpecifier<?>[] rest) {
		OrderSpecifier<?>[] merged = new OrderSpecifier<?>[rest.length + 1];
		merged[0] = first;
		System.arraycopy(rest, 0, merged, 1, rest.length);
		return merged;
	}

	private boolean hasText(String v) {
		return v != null && !v.trim().isEmpty();
	}
}
