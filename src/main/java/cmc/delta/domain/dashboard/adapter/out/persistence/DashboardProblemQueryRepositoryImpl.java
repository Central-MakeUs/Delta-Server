package cmc.delta.domain.dashboard.adapter.out.persistence;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.dashboard.application.dto.DashboardProblemItem;
import cmc.delta.domain.dashboard.application.port.out.DashboardProblemQueryPort;
import cmc.delta.domain.problem.model.problem.QProblem;
import cmc.delta.domain.problem.model.problem.QProblemAiSolutionTask;
import cmc.delta.domain.user.model.QUser;
import com.querydsl.core.types.dsl.CaseBuilder;
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
		QProblemAiSolutionTask task = QProblemAiSolutionTask.problemAiSolutionTask;

		return queryFactory
			.select(constructor(DashboardProblemItem.class,
				problem.id,
				unit.name,
				parentUnit.name,
				type.name,
				new CaseBuilder()
					.when(task.id.isNotNull()).then(1L)
					.otherwise(0L),
				problem.createdAt,
				problem.completedAt.isNotNull(),
				user.role))
			.from(problem)
			.leftJoin(problem.finalUnit, unit)
			.leftJoin(unit.parent, parentUnit)
			.leftJoin(problem.finalType, type)
			.leftJoin(problem.user, user)
			.leftJoin(task).on(task.problem.id.eq(problem.id))
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
			.fetchOne();

		return count != null ? count : 0L;
	}
}
