package cmc.delta.domain.problem.adapter.out.persistence.problem.query.list;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.application.port.in.support.CursorQuery;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemListRow;
import cmc.delta.domain.problem.application.port.out.support.CursorPageResult;
import cmc.delta.domain.problem.model.enums.ProblemListSort;
import cmc.delta.domain.problem.model.problem.QProblem;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemListQuerySupport {

	private final JPAQueryFactory queryFactory;
	private final ProblemListPredicateBuilder predicateBuilder;
	private final ProblemListOrderResolver orderResolver;

	public Page<ProblemListRow> findMyProblemList(Long userId, ProblemListCondition condition, Pageable pageable) {
		Paths p = Paths.create();

		BooleanBuilder where = predicateBuilder.buildMainWhere(userId, condition, p);

		JPAQuery<ProblemListRow> contentQuery = buildContentQuery(p, where);
		contentQuery.orderBy(orderResolver.resolve(userId, condition, p));

		List<ProblemListRow> content = contentQuery
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		long total = fetchTotal(p, where);
		return new PageImpl<>(content, pageable, total);
	}

	public CursorPageResult<ProblemListRow> findMyProblemListCursor(
		Long userId,
		ProblemListCondition condition,
		CursorQuery cursorQuery) {
		Paths p = Paths.create();
		BooleanBuilder baseWhere = predicateBuilder.buildMainWhere(userId, condition, p);
		BooleanBuilder where = new BooleanBuilder(baseWhere);

		applyCursorPredicate(where, condition, cursorQuery, p);

		JPAQuery<ProblemListRow> contentQuery = buildContentQuery(p, where);
		contentQuery.orderBy(orderResolver.resolve(userId, condition, p));

		int size = cursorQuery.size();
		List<ProblemListRow> fetched = contentQuery
			.limit((long)size + 1)
			.fetch();

		boolean hasNext = fetched.size() > size;
		List<ProblemListRow> content = hasNext ? fetched.subList(0, size) : fetched;

		Long nextLastId = null;
		LocalDateTime nextLastCreatedAt = null;
		if (!content.isEmpty()) {
			ProblemListRow last = content.get(content.size() - 1);
			nextLastId = last.problemId();
			nextLastCreatedAt = last.createdAt();
		}

		Long totalElements = cursorQuery.isFirstPage() ? fetchTotal(p, baseWhere) : null;
		return new CursorPageResult<>(content, hasNext, nextLastId, nextLastCreatedAt, totalElements);
	}

	private void applyCursorPredicate(
		BooleanBuilder where,
		ProblemListCondition condition,
		CursorQuery cursorQuery,
		Paths p) {
		if (cursorQuery.isFirstPage()) {
			return;
		}

		Long lastId = cursorQuery.lastId();
		LocalDateTime lastCreatedAt = cursorQuery.lastCreatedAt();
		if (lastId == null || lastCreatedAt == null) {
			throw new IllegalArgumentException("cursor params must include both lastId and lastCreatedAt");
		}

		ProblemListSort sort = (condition.sort() == null) ? ProblemListSort.RECENT : condition.sort();
		if (sort == ProblemListSort.RECENT) {
			where.and(
				p.problem.createdAt.lt(lastCreatedAt)
					.or(p.problem.createdAt.eq(lastCreatedAt).and(p.problem.id.lt(lastId))));
			return;
		}
		if (sort == ProblemListSort.OLDEST) {
			where.and(
				p.problem.createdAt.gt(lastCreatedAt)
					.or(p.problem.createdAt.eq(lastCreatedAt).and(p.problem.id.gt(lastId))));
			return;
		}

		throw new IllegalArgumentException("cursor pagination supports only RECENT/OLDEST");
	}

	private JPAQuery<ProblemListRow> buildContentQuery(Paths p, BooleanBuilder where) {
		return queryFactory
			.select(constructor(
				ProblemListRow.class,
				p.problem.id,
				p.subject.id,
				p.subject.name,
				p.unit.id,
				p.unit.name,
				p.type.id,
				p.type.name,
				p.problem.originalStorageKey,
				p.problem.completedAt,
				p.problem.createdAt))
			.from(p.problem)
			.join(p.problem.finalUnit, p.unit)
			.leftJoin(p.unit.parent, p.subject)
			.join(p.problem.finalType, p.type)
			.where(where);
	}

	private long fetchTotal(Paths p, BooleanBuilder where) {
		Long total = queryFactory
			.select(p.problem.count())
			.from(p.problem)
			.join(p.problem.finalUnit, p.unit)
			.leftJoin(p.unit.parent, p.subject)
			.join(p.problem.finalType, p.type)
			.where(where)
			.fetchOne();

		return total == null ? 0L : total;
	}

	static final class Paths {
		final QProblem problem;
		final QUnit unit;
		final QUnit subject;
		final QProblemType type;

		private Paths(QProblem problem, QUnit unit, QUnit subject, QProblemType type) {
			this.problem = problem;
			this.unit = unit;
			this.subject = subject;
			this.type = type;
		}

		static Paths create() {
			return new Paths(
				QProblem.problem,
				QUnit.unit,
				new QUnit("subject"),
				QProblemType.problemType);
		}
	}
}
