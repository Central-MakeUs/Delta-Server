package cmc.delta.domain.problem.adapter.out.persistence.problem.query.list;

import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.model.problem.QProblem;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemListCountExpressions {

	private final ProblemListPredicateBuilder predicateBuilder;

	public NumberExpression<Long> subjectCount(Long userId, ProblemListCondition condition, StringPath outerSubjectId) {
		QProblem p2 = new QProblem("p2_subject_cnt");

		JPQLQuery<Long> sub = JPAExpressions
			.select(p2.count())
			.from(p2)
			.where(
				predicateBuilder.buildCountBaseWhere(userId, condition, p2)
					.and(p2.finalUnit.parent.id.eq(outerSubjectId))
			);

		return Expressions.numberTemplate(Long.class, "({0})", sub);
	}

	public NumberExpression<Long> unitCount(Long userId, ProblemListCondition condition, StringPath outerUnitId) {
		QProblem p2 = new QProblem("p2_unit_cnt");

		JPQLQuery<Long> sub = JPAExpressions
			.select(p2.count())
			.from(p2)
			.where(
				predicateBuilder.buildCountBaseWhere(userId, condition, p2)
					.and(p2.finalUnit.id.eq(outerUnitId))
			);

		return Expressions.numberTemplate(Long.class, "({0})", sub);
	}

	public NumberExpression<Long> typeCount(Long userId, ProblemListCondition condition, StringPath outerTypeId) {
		QProblem p2 = new QProblem("p2_type_cnt");

		JPQLQuery<Long> sub = JPAExpressions
			.select(p2.count())
			.from(p2)
			.where(
				predicateBuilder.buildCountBaseWhere(userId, condition, p2)
					.and(p2.finalType.id.eq(outerTypeId))
			);

		return Expressions.numberTemplate(Long.class, "({0})", sub);
	}
}
