package cmc.delta.domain.dashboard.adapter.out.persistence;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.dashboard.application.dto.DashboardProblemDetailRow;
import cmc.delta.domain.dashboard.application.dto.DashboardProblemItem;
import cmc.delta.domain.dashboard.application.port.out.DashboardProblemQueryPort;
import cmc.delta.domain.problem.model.problem.QProblem;
import cmc.delta.domain.user.model.QUser;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DashboardProblemQueryRepositoryImpl implements DashboardProblemQueryPort {

	private static final QProblem problem = QProblem.problem;
	private static final QUnit unit = QUnit.unit;
	private static final QUnit parentUnit = new QUnit("parentUnit");
	private static final QProblemType type = QProblemType.problemType;
	private static final QUser user = QUser.user;

	private final JPAQueryFactory queryFactory;

	@Override
	public List<DashboardProblemItem> findProblems(Pageable pageable) {
		return selectProblemRows(constructor(DashboardProblemItem.class, problemColumns()))
			.orderBy(problem.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
	}

	@Override
	public long countProblems() {
		Long count = queryFactory
			.select(problem.id.count())
			.from(problem)
			.fetchOne();

		return count != null ? count : 0L;
	}

	@Override
	public Optional<DashboardProblemDetailRow> findProblemDetail(Long problemId) {
		DashboardProblemDetailRow row = selectProblemRows(
			constructor(DashboardProblemDetailRow.class, problemColumns(problem.originalStorageKey)))
			.where(problem.id.eq(problemId))
			.fetchOne();

		return Optional.ofNullable(row);
	}

	/** 목록/상세가 공유하는 공통 프로젝션 컬럼에 추가 컬럼을 덧붙인다. */
	private Expression<?>[] problemColumns(Expression<?>... extraColumns) {
		List<Expression<?>> columns = new ArrayList<>(List.of(
			problem.id,
			unit.name,
			parentUnit.name,
			type.name,
			problem.aiSolutionCount.longValue(),
			problem.viewCount.longValue(),
			problem.createdAt,
			problem.completedAt.isNotNull(),
			user.role));
		columns.addAll(List.of(extraColumns));
		return columns.toArray(new Expression<?>[0]);
	}

	private <T> JPAQuery<T> selectProblemRows(Expression<T> projection) {
		return queryFactory
			.select(projection)
			.from(problem)
			.leftJoin(problem.finalUnit, unit)
			.leftJoin(unit.parent, parentUnit)
			.leftJoin(problem.finalType, type)
			.leftJoin(problem.user, user);
	}

}
