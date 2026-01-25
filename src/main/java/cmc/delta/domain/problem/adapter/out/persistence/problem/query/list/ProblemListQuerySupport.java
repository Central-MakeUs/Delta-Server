package cmc.delta.domain.problem.adapter.out.persistence.problem.query.list;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemListRow;
import cmc.delta.domain.problem.model.asset.QAsset;
import cmc.delta.domain.problem.model.enums.AssetType;
import cmc.delta.domain.problem.model.problem.QProblem;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
				p.asset.id,
				p.asset.storageKey,
				p.problem.createdAt
			))
			.from(p.problem)
			.join(p.problem.finalUnit, p.unit)
			.leftJoin(p.unit.parent, p.subject)
			.join(p.problem.finalType, p.type)
			.join(p.asset).on(
				p.asset.scan.id.eq(p.problem.scan.id)
					.and(p.asset.assetType.eq(AssetType.ORIGINAL))
			)
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
		final QAsset asset;

		private Paths(QProblem problem, QUnit unit, QUnit subject, QProblemType type, QAsset asset) {
			this.problem = problem;
			this.unit = unit;
			this.subject = subject;
			this.type = type;
			this.asset = asset;
		}

		static Paths create() {
			return new Paths(
				QProblem.problem,
				QUnit.unit,
				new QUnit("subject"),
				QProblemType.problemType,
				QAsset.asset
			);
		}
	}
}
