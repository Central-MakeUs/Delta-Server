package cmc.delta.domain.dashboard.adapter.out.persistence;

import cmc.delta.domain.dashboard.application.port.out.DashboardMonthlyAccessQueryPort;
import cmc.delta.domain.stats.model.QUserDailyAccess;
import cmc.delta.domain.user.model.QUser;
import cmc.delta.domain.user.model.enums.UserRole;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
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
		LocalDate start = monthStart(yearMonth);
		LocalDate end = yearMonth.atEndOfMonth();

		return queryFactory
			.select(access.accessDate, access.userId.countDistinct())
			.from(access)
			.where(
				access.accessDate.between(start, end),
				excludeAdminAccess(access))
			.groupBy(access.accessDate)
			.fetch()
			.stream()
			.collect(Collectors.toMap(
				t -> t.get(access.accessDate),
				t -> t.get(access.userId.countDistinct())));
	}

	@Override
	public Map<LocalDate, Long> findDailyNewUsersByMonth(YearMonth yearMonth) {
		QUser user = QUser.user;
		DateTemplate<java.sql.Date> signupDate = Expressions.dateTemplate(
			java.sql.Date.class,
			"DATE({0})",
			user.createdAt);

		List<Tuple> results = fetchDailySignups(yearMonth, user, signupDate);
		return results.stream()
			.collect(Collectors.toMap(
				t -> t.get(signupDate).toLocalDate(),
				t -> t.get(user.id.count())));
	}

	private List<Tuple> fetchDailySignups(YearMonth yearMonth, QUser user, DateTemplate<java.sql.Date> signupDate) {
		return queryFactory
			.select(signupDate, user.id.count())
			.from(user)
			.where(
				user.createdAt.goe(monthStart(yearMonth).atStartOfDay()),
				user.createdAt.lt(monthStart(yearMonth.plusMonths(1)).atStartOfDay()),
				user.role.ne(UserRole.ADMIN))
			.groupBy(signupDate)
			.fetch();
	}

	private LocalDate monthStart(YearMonth yearMonth) {
		return yearMonth.atDay(1);
	}

	// 접속 기록에는 role이 없어 ADMIN ID 목록을 선조회해 제외한다.
	private BooleanExpression excludeAdminAccess(QUserDailyAccess access) {
		List<Long> adminIds = fetchAdminUserIds();
		return adminIds.isEmpty() ? null : access.userId.notIn(adminIds);
	}

	// 1. ADMIN ID 목록 선조회
	private List<Long> fetchAdminUserIds() {
		QUser user = QUser.user;
		return queryFactory
			.select(user.id)
			.from(user)
			.where(user.role.eq(UserRole.ADMIN))
			.fetch();
	}
}
