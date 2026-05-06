package cmc.delta.domain.dashboard.adapter.out.persistence;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.dashboard.application.dto.DashboardUserItem;
import cmc.delta.domain.dashboard.application.port.out.DashboardUserQueryPort;
import cmc.delta.domain.problem.model.problem.QProblem;
import cmc.delta.domain.stats.model.QUserDailyAccess;
import cmc.delta.domain.user.model.QUser;
import cmc.delta.domain.user.model.enums.UserRole;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DashboardUserQueryRepositoryImpl implements DashboardUserQueryPort {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<DashboardUserItem> findUsers(int page, int size) {
		QUser user = QUser.user;
		QUserDailyAccess access = QUserDailyAccess.userDailyAccess;
		QProblem problem = QProblem.problem;

		return queryFactory
			.select(constructor(DashboardUserItem.class,
				user.id,
				user.nickname,
				user.role,
				access.accessDate.countDistinct(),
				access.accessDate.max(),
				problem.id.countDistinct()))
			.from(user)
			.leftJoin(access).on(access.user.id.eq(user.id))
			.leftJoin(problem).on(problem.user.id.eq(user.id))
			.where(user.role.ne(UserRole.ADMIN))
			.groupBy(user.id, user.nickname)
			.orderBy(user.id.desc())
			.offset((long) page * size)
			.limit(size)
			.fetch();
	}

	@Override
	public long countUsers() {
		QUser user = QUser.user;

		Long count = queryFactory
			.select(user.id.count())
			.from(user)
			.where(user.role.ne(UserRole.ADMIN))
			.fetchOne();

		return count != null ? count : 0L;
	}
}
