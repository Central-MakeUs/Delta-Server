package cmc.delta.domain.dashboard.adapter.out.persistence;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.dashboard.application.dto.DashboardProblemItem;
import cmc.delta.domain.dashboard.application.port.out.DashboardProblemQueryPort;
import cmc.delta.domain.problem.model.problem.QProblem;
import cmc.delta.domain.user.model.QUser;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DashboardProblemQueryRepositoryImpl implements DashboardProblemQueryPort {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<DashboardProblemItem> findProblems(Pageable pageable) {
		QProblem problem = QProblem.problem;
		QUnit unit = QUnit.unit;
		QUnit parentUnit = new QUnit("parentUnit");
		QProblemType type = QProblemType.problemType;
		QUser user = QUser.user;

		return queryFactory
			.select(constructor(DashboardProblemItem.class,
				problem.id,
				unit.name,
				parentUnit.name,
				type.name,
				problem.aiSolutionCount.longValue(),
				problem.viewCount.longValue(),
				problem.createdAt,
				problem.completedAt.isNotNull(),
				user.role))
			.from(problem)
			.leftJoin(problem.finalUnit, unit)
			.leftJoin(unit.parent, parentUnit)
			.leftJoin(problem.finalType, type)
			.leftJoin(problem.user, user)
			.where(getCommonWhereConditions())
			.orderBy(problem.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
	}

	@Override
	public long countProblems() {
		QProblem problem = QProblem.problem;

		Long count = queryFactory
			.select(problem.id.count())
			.from(problem)
			.where(getCommonWhereConditions())
			.fetchOne();

		return count != null ? count : 0L;
	}

	private BooleanExpression[] getCommonWhereConditions() {
		QProblem problem = QProblem.problem;

		return new BooleanExpression[] {
			// 지금은 조건이 없으므로 빈 배열을 반환하거나 null을 반환하도록 설계합니다.
		};
	}
}
