package cmc.delta.domain.problem.adapter.out.persistence.problem.query.list;

import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import cmc.delta.domain.problem.model.problem.QProblem;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemListPredicateBuilder {

	public BooleanBuilder buildMainWhere(
		Long userId,
		ProblemListCondition condition,
		ProblemListQuerySupport.Paths p
	) {
		BooleanBuilder where = new BooleanBuilder();
		where.and(p.problem.user.id.eq(userId));

		if (hasText(condition.unitId())) {
			where.and(p.unit.id.eq(condition.unitId()));
		}
		if (hasText(condition.typeId())) {
			where.and(p.type.id.eq(condition.typeId()));
		}
		if (hasText(condition.subjectId())) {
			where.and(p.subject.id.eq(condition.subjectId()));
		}

		applyStatusFilter(where, condition.status(), p.problem);
		return where;
	}

	public BooleanBuilder buildCountBaseWhere(Long userId, ProblemListCondition condition, QProblem p2) {
		BooleanBuilder booleanBuilder = new BooleanBuilder();
		booleanBuilder.and(p2.user.id.eq(userId));

		if (hasText(condition.subjectId())) {
			booleanBuilder.and(p2.finalUnit.parent.id.eq(condition.subjectId()));
		}
		if (hasText(condition.unitId())) {
			booleanBuilder.and(p2.finalUnit.id.eq(condition.unitId()));
		}
		if (hasText(condition.typeId())) {
			booleanBuilder.and(p2.finalType.id.eq(condition.typeId()));
		}

		// 최다/최소 등록순은 완료 여부와 무관하게 계산하려면 status는 여기서 일부러 제외한다.
		return booleanBuilder;
	}

	private void applyStatusFilter(BooleanBuilder where, ProblemStatusFilter status, QProblem problem) {
		if (status == null || status == ProblemStatusFilter.ALL) return;

		if (status == ProblemStatusFilter.SOLVED) {
			where.and(problem.completedAt.isNotNull());
			return;
		}

		if (status == ProblemStatusFilter.UNSOLVED) {
			where.and(problem.completedAt.isNull());
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
