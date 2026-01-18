package cmc.delta.domain.problem.persistence.problem;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.api.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.model.problem.QProblem;
import cmc.delta.domain.problem.persistence.problem.dto.ProblemListCondition;
import cmc.delta.domain.problem.persistence.problem.dto.ProblemListRow;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProblemQueryRepositoryImpl implements ProblemQueryRepository {

	private static final int PREVIEW_LENGTH = 80;

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<ProblemListRow> findMyProblemList(Long userId, ProblemListCondition condition, Pageable pageable) {
		QProblem problem = QProblem.problem;
		QUnit unit = QUnit.unit;
		QUnit subject = new QUnit("subject");
		QProblemType type = QProblemType.problemType;

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

		StringExpression markdownAsString =
			Expressions.stringTemplate("cast({0} as string)", problem.problemMarkdown);

		StringExpression previewExpr =
			Expressions.stringTemplate("substring({0}, {1}, {2})", markdownAsString, 1, PREVIEW_LENGTH);

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
				previewExpr,
				problem.createdAt
			))
			.from(problem)
			.join(problem.finalUnit, unit)
			.leftJoin(unit.parent, subject)
			.join(problem.finalType, type)
			.where(where);

		applySort(contentQuery, condition.sort(), problem);

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

	private void applySort(JPAQuery<ProblemListRow> query, ProblemListSort sort, QProblem problem) {
		if (sort == ProblemListSort.OLDEST) {
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
