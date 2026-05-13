package cmc.delta.domain.dashboard.adapter.out.persistence;

import cmc.delta.domain.dashboard.application.port.out.DashboardMonthlyAccessQueryPort;
import cmc.delta.domain.stats.model.QUserDailyAccess;
import cmc.delta.domain.user.model.QUser;
import cmc.delta.domain.user.model.enums.UserRole;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DashboardMonthlyAccessQueryRepositoryImpl implements DashboardMonthlyAccessQueryPort {

	private final JPAQueryFactory queryFactory;

	@Override
	public Map<LocalDate, Long> findDailyAccessByMonth(YearMonth yearMonth) {
		QUserDailyAccess access = QUserDailyAccess.userDailyAccess;
		QUser user = QUser.user;
		LocalDate start = yearMonth.atDay(1);
		LocalDate end = yearMonth.atEndOfMonth();

		return queryFactory
			.select(access.accessDate, access.userId.countDistinct())
			.from(access)
			.join(user).on(user.id.eq(access.userId))
			.where(
				access.accessDate.between(start, end),
				user.role.ne(UserRole.ADMIN)
			)
			.groupBy(access.accessDate)
			.fetch()
			.stream()
			.collect(Collectors.toMap(
				t -> t.get(access.accessDate),
				t -> t.get(access.userId.countDistinct())
			));
	}

	@Override
	public Map<LocalDate, Long> findDailyNewUsersByMonth(YearMonth yearMonth) {
		QUser user = QUser.user;
		LocalDate start = yearMonth.atDay(1);

		DateTemplate<LocalDate> signupDate = Expressions.dateTemplate(
			LocalDate.class, "DATE({0})", user.createdAt);

		List<Tuple> results = queryFactory
			.select(signupDate, user.id.count())
			.from(user)
			.where(
				user.createdAt.goe(start.atStartOfDay()),
				user.createdAt.lt(yearMonth.plusMonths(1).atDay(1).atStartOfDay()),
				user.role.ne(UserRole.ADMIN)
			)
			.groupBy(signupDate)
			.fetch();

		return results.stream()
			.collect(Collectors.toMap(
				t -> t.get(signupDate),
				t -> t.get(user.id.count())
			));
	}
}
